// Supabase Edge Function: send-license-otp
// Lokasi deploy: supabase/functions/send-license-otp/index.ts
// Environment Variable yang dibutuhkan di Supabase Secrets:
// - RESEND_API_KEY: API Key dari https://resend.com (Gratis 3.000 email/bulan)
// - SUPABASE_URL: URL proyek Supabase Anda
// - SUPABASE_SERVICE_ROLE_KEY: Service Role Key Supabase Anda

import { serve } from "https://deno.land/std@0.168.0/http/server.ts";
import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
};

serve(async (req: Request) => {
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders });
  }

  try {
    const { license_key, email } = await req.json();

    if (!license_key || !email) {
      return new Response(
        JSON.stringify({ success: false, message: "license_key dan email wajib diisi." }),
        { status: 400, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    const cleanKey = String(license_key).trim().toUpperCase();
    const cleanEmail = String(email).trim().toLowerCase();

    // 1. Generate 6-digit OTP
    const otp = Math.floor(100000 + Math.random() * 900000).toString();

    // 2. Hubungi Supabase DB via Service Role Client
    const supabaseUrl = Deno.env.get("SUPABASE_URL") ?? "";
    const supabaseKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") ?? "";
    const supabase = createClient(supabaseUrl, supabaseKey);

    const { data, error } = await supabase.rpc("request_license_otp", {
      p_license_key: cleanKey,
      p_email: cleanEmail,
      p_otp: otp,
      p_expires_minutes: 15,
    });

    if (error || !data?.success) {
      return new Response(
        JSON.stringify({ success: false, message: data?.message || error?.message || "Gagal membuat OTP." }),
        { status: 400, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    // 3. Kirim Email via Resend API
    const resendApiKey = Deno.env.get("RESEND_API_KEY");
    if (resendApiKey) {
      const emailHtml = `
        <div style="font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; background-color: #0C0E12; color: #F5F8FF; padding: 32px; border-radius: 12px; max-width: 500px; margin: 0 auto; border: 1px solid #222B38;">
          <div style="text-align: center; margin-bottom: 24px;">
            <h1 style="color: #E5A93C; margin: 0; font-size: 26px; font-weight: 800; letter-spacing: -0.5px;">KordApp</h1>
            <p style="color: #7D8A99; font-size: 13px; margin-top: 4px;">Verifikasi Email Pemulihan Lisensi</p>
          </div>
          <div style="background-color: #13171E; border-radius: 8px; padding: 24px; border: 1px solid #222B38; text-align: center;">
            <p style="font-size: 14px; color: #D2D9E5; margin-bottom: 16px;">
              Gunakan kode 6-digit di bawah ini untuk menautkan email Anda ke lisensi KordApp (<strong>${cleanKey}</strong>):
            </p>
            <div style="font-size: 36px; font-weight: 800; letter-spacing: 8px; color: #E5A93C; background: rgba(229, 169, 60, 0.12); padding: 12px; border-radius: 8px; border: 1px solid rgba(229, 169, 60, 0.3); display: inline-block; font-family: monospace;">
              ${otp}
            </div>
            <p style="font-size: 12px; color: #7D8A99; margin-top: 16px;">
              Kode ini berlaku selama <strong>15 menit</strong>. Jangan berikan kode ini kepada siapapun.
            </p>
          </div>
          <p style="font-size: 11px; color: #4B5563; text-align: center; margin-top: 24px;">
            Jika Anda tidak meminta verifikasi ini, silakan abaikan email ini.
          </p>
        </div>
      `;

      const resendRes = await fetch("https://api.resend.com/emails", {
        method: "POST",
        headers: {
          "Authorization": `Bearer ${resendApiKey}`,
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          from: "KordApp Support <onboarding@resend.dev>", // Ganti dengan domain verified Anda (misal noreply@kordapp.id)
          to: [cleanEmail],
          subject: `Kode Verifikasi KordApp: ${otp}`,
          html: emailHtml,
        }),
      });

      if (!resendRes.ok) {
        const errBody = await resendRes.text();
        console.error("Resend API error:", errBody);
      }
    }

    return new Response(
      JSON.stringify({ 
        success: true, 
        message: "Kode OTP telah dikirim ke email " + cleanEmail 
      }),
      { status: 200, headers: { ...corsHeaders, "Content-Type": "application/json" } }
    );
  } catch (err) {
    return new Response(
      JSON.stringify({ success: false, message: (err as Error).message }),
      { status: 500, headers: { ...corsHeaders, "Content-Type": "application/json" } }
    );
  }
});
