import pathlib
import re

root = pathlib.Path(r"C:\Users\xiaohongfu\IdeaProjects\travel\backend\route-service\src\main\java\travel\route\dto\ai")

def split_type_and_name(field_line: str):
    m = re.match(r'private\s+(.+?)\s+(\w+)\s*;', field_line.strip())
    if not m:
        return None
    return m.group(1), m.group(2)

for path in sorted(root.glob('*.java')):
    text = path.read_text(encoding='utf-8')
    lines = text.splitlines()
    package_line = next((line for line in lines if line.startswith('package ')), None)
    if package_line is None:
        continue

    import_lines = []
    for line in lines:
        if line.startswith('import '):
            if 'lombok' not in line:
                import_lines.append(line)

    class_match = re.search(r'public\s+class\s+(\w+)\s*\{', text)
    if not class_match:
        continue
    class_name = class_match.group(1)

    body_start = text.index('{', class_match.start()) + 1
    body_end = text.rfind('}')
    body = text[body_start:body_end]
    body_lines = body.splitlines()

    fields = []
    pending_annotations = []
    for raw_line in body_lines:
        line = raw_line.rstrip()
        stripped = line.strip()
        if not stripped:
            continue
        if stripped.startswith('@'):
            if 'lombok' in stripped:
                continue
            if stripped in ('@Data', '@Builder', '@NoArgsConstructor', '@AllArgsConstructor'):
                continue
            pending_annotations.append(line)
            continue
        if stripped.startswith('private '):
            parsed = split_type_and_name(stripped)
            if parsed is None:
                pending_annotations = []
                continue
            field_type, field_name = parsed
            fields.append((pending_annotations[:], field_type, field_name))
            pending_annotations = []
            continue
        pending_annotations = []

    if not fields:
        continue

    imports = []
    needed = set()
    for _, field_type, _ in fields:
        if 'List<' in field_type or field_type == 'List':
            needed.add('java.util.List')
        if 'Map<' in field_type or field_type == 'Map':
            needed.add('java.util.Map')
        if 'LocalDateTime' in field_type:
            needed.add('java.time.LocalDateTime')
    for imp in import_lines:
        imports.append(imp)
    for type_import in sorted(needed):
        imp_line = f'import {type_import};'
        if imp_line not in imports:
            imports.append(imp_line)

    out = [package_line, '']
    if imports:
        out.extend(imports)
        out.append('')
    out.append(f'public class {class_name} '+'{')
    out.append('')

    for annotations, field_type, field_name in fields:
        for ann in annotations:
            out.append(f'    {ann}')
        out.append(f'    private {field_type} {field_name};')
        out.append('')

    # constructors
    out.append(f'    public {class_name}() '+'{')
    out.append('    }')
    out.append('')
    params = ', '.join(f'{field_type} {field_name}' for _, field_type, field_name in fields)
    out.append(f'    public {class_name}({params}) '+'{')
    for _, _, field_name in fields:
        out.append(f'        this.{field_name} = {field_name};')
    out.append('    }')
    out.append('')

    # getters/setters
    for _, field_type, field_name in fields:
        suffix = field_name[0].upper() + field_name[1:]
        out.append(f'    public {field_type} get{suffix}() '+'{')
        out.append(f'        return {field_name};')
        out.append('    }')
        out.append('')
        out.append(f'    public void set{suffix}({field_type} {field_name}) '+'{')
        out.append(f'        this.{field_name} = {field_name};')
        out.append('    }')
        out.append('')

    out.append('}')
    out_text = '\n'.join(out) + '\n'
    path.write_text(out_text, encoding='utf-8')
    print(f'Rewrote {path.name} with {len(fields)} fields')