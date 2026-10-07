$dbs = @(
    @{ Name = "UG"; File = "catalog\UG.db" },
    @{ Name = "GT"; File = "catalog\GT.db" },
    @{ Name = "ChordTela"; File = "catalog\Chordtela.db" }
)

foreach ($db in $dbs) {
    Write-Host ""
    Write-Host "========================================"
    Write-Host $db.Name
    Write-Host "========================================"

    $sql = @"
SELECT
    COUNT(*) AS total_rows,
    COUNT(DISTINCT artist_slug || '|' || title_slug) AS unique_songs,
    COUNT(DISTINCT artist_slug) AS unique_artists
FROM songs;
"@

    & C:\sqlite\sqlite3.exe -header -column $db.File $sql

    Write-Host ""
    Write-Host "-- NULL / EMPTY METADATA --"

    if ($db.Name -eq "UG") {
        $sql = @"
SELECT
    SUM(CASE WHEN version_name IS NULL OR TRIM(version_name) = '' THEN 1 ELSE 0 END) AS no_version,
    SUM(CASE WHEN rating IS NULL THEN 1 ELSE 0 END) AS no_rating,
    SUM(CASE WHEN votes IS NULL THEN 1 ELSE 0 END) AS no_votes,
    SUM(CASE WHEN key_base IS NULL OR TRIM(key_base) = '' THEN 1 ELSE 0 END) AS no_key,
    SUM(CASE WHEN difficulty IS NULL OR TRIM(difficulty) = '' THEN 1 ELSE 0 END) AS no_difficulty
FROM songs;
"@
    }
    elseif ($db.Name -eq "GT") {
        $sql = @"
SELECT
    SUM(CASE WHEN version_name IS NULL OR TRIM(version_name) = '' THEN 1 ELSE 0 END) AS no_version,
    SUM(CASE WHEN difficulty IS NULL OR TRIM(difficulty) = '' THEN 1 ELSE 0 END) AS no_difficulty
FROM songs;
"@
    }
    else {
        $sql = @"
SELECT
    SUM(CASE WHEN category IS NULL OR TRIM(category) = '' THEN 1 ELSE 0 END) AS no_category
FROM songs;
"@
    }

    & C:\sqlite\sqlite3.exe -header -column $db.File $sql

    Write-Host ""
    Write-Host "-- REGIONS --"

    & C:\sqlite\sqlite3.exe -header -column $db.File `
        "SELECT COALESCE(region,'[NULL]') AS region, COUNT(*) AS songs FROM songs GROUP BY region ORDER BY songs DESC;"
}