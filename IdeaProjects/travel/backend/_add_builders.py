import pathlib
import re

root = pathlib.Path(r"C:\Users\xiaohongfu\IdeaProjects\travel\backend\route-service\src\main\java\travel\route\dto\ai")
for path in sorted(root.glob('*.java')):
    text = path.read_text(encoding='utf-8')
    if 'static class Builder' in text or 'public static Builder builder()' in text:
        continue
    class_match = re.search(r'public\s+class\s+(\w+)\s*\{', text)
    if not class_match:
        continue
    class_name = class_match.group(1)
    body_start = text.index('{', class_match.start()) + 1
    body_end = text.rfind('}')
    body = text[body_start:body_end]
    fields = []
    for line in body.splitlines():
        stripped = line.strip()
        if stripped.startswith('private ') and stripped.endswith(';'):
            m = re.match(r'private\s+(.+?)\s+(\w+)\s*;', stripped)
            if m:
                fields.append((m.group(1), m.group(2)))
    if not fields:
        continue
    builder = []
    builder.append(f'    public static Builder builder() '+'{')
    builder.append('        return new Builder();')
    builder.append('    }')
    builder.append('')
    builder.append(f'    public static class Builder '+'{')
    for field_type, field_name in fields:
        builder.append(f'        private {field_type} {field_name};')
    builder.append('')
    for field_type, field_name in fields:
        method_name = field_name[0].upper() + field_name[1:]
        builder.append(f'        public Builder {field_name}({field_type} {field_name}) '+'{')
        builder.append(f'            this.{field_name} = {field_name};')
        builder.append('            return this;')
        builder.append('        }')
        builder.append('')
    builder.append(f'        public {class_name} build() '+'{')
    params = ', '.join(f'{field_name}' for _, field_name in fields)
    builder.append(f'            return new {class_name}({params});')
    builder.append('        }')
    builder.append('    }')
    builder.append('')

    new_text = text[:body_end].rstrip() + '\n\n' + '\n'.join(builder) + '\n}'
    path.write_text(new_text, encoding='utf-8')
    print(f'Added builder to {path.name}')