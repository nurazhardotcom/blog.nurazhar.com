import os
import re
import glob
import sys

public_dir = 'public'
if not os.path.exists(public_dir):
    print(f"Error: {public_dir} directory does not exist. Run 'bb build' first.")
    sys.exit(1)

existing_files = set()
for root, dirs, files in os.walk(public_dir):
    for f in files:
        rel_path = os.path.relpath(os.path.join(root, f), public_dir)
        existing_files.add(rel_path)

broken_links = []
md_links = []

# Regex to find href="..." and src="..."
href_re = re.compile(r'(?:href|src)=["\']([^"\']+)["\']', re.IGNORECASE)

html_files = glob.glob(os.path.join(public_dir, '**/*.html'), recursive=True)

for html_file in html_files:
    rel_html_path = os.path.relpath(html_file, public_dir)
    with open(html_file, 'r', encoding='utf-8', errors='ignore') as f:
        content = f.read()
    
    matches = href_re.findall(content)
    for url in matches:
        # Ignore external links, anchors, inline data, mail/tel protocols, and placeholder triple dots
        if url.startswith(('http://', 'https://', 'mailto:', 'tel:', '#', 'data:')) or url == '...':
            continue
        clean_url = url.split('?')[0].split('#')[0]
        if not clean_url or clean_url == '...':
            continue
        if clean_url.endswith('.md'):
            md_links.append((rel_html_path, url))
            
        html_dir = os.path.dirname(rel_html_path)
        target_path = os.path.normpath(os.path.join(html_dir, clean_url))
        if target_path not in existing_files:
            # Verify if the file actually exists on disk in public
            full_target_path = os.path.normpath(os.path.join(public_dir, target_path))
            if not os.path.exists(full_target_path):
                broken_links.append((rel_html_path, url, target_path))

print(f"Validated {len(html_files)} HTML files.")
print("MD Links count:", len(md_links))
if md_links:
    for src, url in md_links:
        print(f"  - In {src}: link to {url}")

print("Broken Links count:", len(broken_links))
if broken_links:
    for src, url, target in broken_links:
        print(f"  - In {src}: broken reference {url} (expected at {target})")
    sys.exit(1)
else:
    print("✅ All internal links are working perfectly!")
    sys.exit(0)
