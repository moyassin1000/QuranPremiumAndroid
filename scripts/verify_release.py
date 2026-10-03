from pathlib import Path
import re, sys

root = Path(__file__).resolve().parents[1]
app_gradle = (root / 'app/build.gradle.kts').read_text(encoding='utf-8')
errors = []

def must(pattern, label):
    if not re.search(pattern, app_gradle):
        errors.append(f'Missing/invalid {label}')

must(r'applicationId\s*=\s*"com\.qurankareem\.app"', 'applicationId')
must(r'versionCode\s*=\s*8\b', 'versionCode 8')
must(r'versionName\s*=\s*"1\.7\.0"', 'versionName 1.7.0')
must(r'targetSdk\s*=\s*36\b', 'targetSdk 36')

for path in root.rglob('*'):
    if not path.is_file():
        continue
    rel = path.relative_to(root).as_posix()
    lower = path.name.lower()
    if lower.endswith(('.jks', '.keystore')):
        errors.append(f'Signing key must not be committed: {rel}')
    if path.stat().st_size > 5_000_000:
        continue
    if path.suffix.lower() in {'.kt', '.kts', '.xml', '.md', '.properties', '.yml', '.yaml', '.txt'}:
        text = path.read_text(encoding='utf-8', errors='ignore')
        if '-----BEGIN PRIVATE KEY-----' in text or '-----BEGIN RSA PRIVATE KEY-----' in text:
            errors.append(f'Private key material found: {rel}')

manifest = root / 'app/src/main/AndroidManifest.xml'
if not manifest.exists():
    errors.append('AndroidManifest.xml missing')

if errors:
    print('\n'.join(f'ERROR: {e}' for e in errors))
    sys.exit(1)
print('Release verification passed: identity, versioning, manifest, and secret-file checks are clean.')
