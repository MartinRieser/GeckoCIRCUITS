import os
import re
import glob

reviews_dir = os.path.dirname(os.path.abspath(__file__))
targets_dir = os.path.join(reviews_dir, "targets")
os.makedirs(targets_dir, exist_ok=True)

# Find all 01-*.md to 13-*.md files
review_files = glob.glob(os.path.join(reviews_dir, "[0-9][0-9]-*.md"))
review_files = [f for f in review_files if not f.endswith("00-INDEX.md")]

# Read all Java files list
all_java_files = []
all_java_files_path = os.path.join(os.path.dirname(reviews_dir), "all_java_files.txt")
if os.path.exists(all_java_files_path):
    with open(all_java_files_path, "r", encoding="utf-8") as f:
        for line in f:
            line = line.strip()
            if line:
                all_java_files.append(line)

def get_full_path(filename):
    for p in all_java_files:
        if p.endswith('/' + filename) or p.endswith('\\' + filename):
            normalized = p.replace('\\', '/')
            if re.match(r'^/[a-zA-Z]/', normalized):
                normalized = normalized[1:2] + ':/' + normalized[3:]
            elif normalized.startswith('/'):
                normalized = normalized[1:]
            return normalized
    return filename

def categorize_bullet(bullet):
    b = bullet.lower()
    
    # Ignore bullets that indicate no work is needed
    if "no improvements" in b or "no task" in b or "looks clean" in b:
        return None

    # Phase 4: Assert-to-Exception
    if any(kw in b for kw in ["assert false", "assert-based", "assertion-based", "asserts can", "replace assert", "contradictory assert"]):
        return 4
        
    # Phase 5: Test Cases
    if any(kw in b for kw in ["test case", "untested class", "test coverage", "junit"]):
        return 5
        
    # Phase 6: Bug fixes, logic issues, NPEs, self-assignments, shadowed variables
    if any(kw in b for kw in [
        "bug", "npe", "null pointer", "copy-paste error", "duplicate zoom", 
        "shadowed variable", "self-assignment", "isinfinite", "array hash", 
        "stack trace", "shadowed", "redundant zoom", "zoom condition", 
        "correctwithnotanumber", "output count mismatch", "classbytes", 
        "index bounds", "infinite values", "is memory restart required",
        "input validation", "numberformatexception", "null check in firstelement"
    ]):
        return 6
        
    # Phase 3: Magic numbers & indices
    if any(kw in b for kw in [
        "magic number", "magic index", "magic constant", "hardcoded index", 
        "hardcoded value", "hardcoded constant", "bare numeric", "sentinel", 
        "magic-number", "magic defaults", "tab indices", "iparm"
    ]):
        return 3
        
    # Phase 7: German to English translations
    if any(kw in b for kw in ["german", "translation", "translate", "alias", "english"]):
        return 7
        
    # Phase 1: Dead code, debug prints, redundant code, commented out code, style/formatting
    if any(kw in b for kw in [
        "dead code", "commented-out", "unused import", "system.out.print", 
        "system.err.print", "println", "print statement", "debug print", 
        "unused variable", "unused local", "remove commented", "redundant null check", 
        "duplicate check", "redundant check", "unused field", "never referenced", 
        "extra blank line", "identical duplicate", "remove redundant", "unused param", 
        "unused orientierung", "empty jtextfield", "indentation", "blank lines", 
        "trailing semicolon", "unused method", "unused constructor", "remove empty",
        "remove unused"
    ]):
        return 1

    # Phase 2: Typos and renames
    if any(kw in b for kw in [
        "typo", "misspell", "spelling", "misspelled", "double semicolon", 
        "rename", "naming convention"
    ]):
        return 2

    # Phase 8: Javadoc & Documentation
    if any(kw in b for kw in [
        "javadoc", "document ", "comment", "explain", "docstring", 
        "@param", "@return", "@deprecated", "@throws", "@exception", 
        "documentation", "class-level note", "add note"
    ]):
        return 8
        
    # Fallback
    return 9

parsed_data = {}
for filepath in review_files:
    with open(filepath, "r", encoding="utf-8") as f:
        content = f.read()
    
    sections = re.split(r'^##\s+', content, flags=re.MULTILINE)
    for sec in sections[1:]:
        lines = sec.strip().split('\n')
        if not lines:
            continue
        file_header = lines[0].strip()
        bullets = []
        for line in lines[1:]:
            line_strip = line.strip()
            if line_strip.startswith('-') or line_strip.startswith('*'):
                bullet_text = re.sub(r'^[-*]\s+', '', line_strip)
                bullets.append(bullet_text)
        if bullets:
            parsed_data[file_header] = bullets

# Categorize
categorized = {i: {} for i in range(1, 10)}

for file_header, bullets in parsed_data.items():
    for bullet in bullets:
        phase = categorize_bullet(bullet)
        if phase is None:
            continue
        if file_header not in categorized[phase]:
            categorized[phase][file_header] = []
        categorized[phase][file_header].append(bullet)

# Handle Phase 9 fallback merges
for file_header, bullets in list(categorized[9].items()):
    for bullet in bullets:
        b = bullet.lower()
        if any(kw in b for kw in ["refactor", "duplication", "make class final", "public to private", "validation", "redundant"]):
            dest = 6
        elif any(kw in b for kw in ["javadoc", "comment", "document"]):
            dest = 8
        else:
            dest = 1
        if file_header not in categorized[dest]:
            categorized[dest][file_header] = []
        categorized[dest][file_header].append(bullet)
del categorized[9]

# Now, write the markdown files for each phase (except phase 5 which is handled separately)
phase_names = {
    1: "Phase 1: Dead Code, Debug Prints, and Redundant Code Removal",
    2: "Phase 2: Typo Fixes and Naming Standardizations",
    3: "Phase 3: Magic Number and Sentinel Value Extraction",
    4: "Phase 4: Assert-to-Exception Fixes",
    6: "Phase 6: Logic, Shadowing, and Bug Fixes",
    7: "Phase 7: German-to-English Translations and Aliases",
    8: "Phase 8: Javadoc and Code Documentation"
}

phase_filenames = {
    1: "phase1_dead_code.md",
    2: "phase2_typo_fixes.md",
    3: "phase3_magic_numbers.md",
    4: "phase4_assert_fixes.md",
    6: "phase6_bug_fixes.md",
    7: "phase7_translations.md",
    8: "phase8_javadoc.md"
}

for phase, name in phase_names.items():
    data = categorized[phase]
    filename = phase_filenames[phase]
    filepath = os.path.join(targets_dir, filename)
    
    sorted_files = sorted(data.keys())
    
    with open(filepath, "w", encoding="utf-8") as f:
        f.write(f"# {name}\n\n")
        f.write(f"This file lists all the target files and specific tasks for {name} parsed from the review files.\n\n")
        f.write(f"Total target files: {len(sorted_files)}\n\n")
        f.write("## File and Task List\n\n")
        
        for file_hdr in sorted_files:
            full_path = get_full_path(file_hdr)
            file_link = f"[{file_hdr}](file:///{full_path})"
            f.write(f"### {file_link}\n")
            for bullet in data[file_hdr]:
                f.write(f"- {bullet}\n")
            f.write("\n")

print("Generated target files in: improvement-reviews/targets/")
for phase, name in phase_names.items():
    print(f"- {phase_filenames[phase]}: {len(categorized[phase])} files")
