#!/usr/bin/env python3
"""
Tidies the imports of every Java file: drops unused ones, sorts the rest into groups
(static; java/javax; com/org/android; ohheck), one blank line between groups.

    python3 tools/dev/imports.py
"""
import os
os.chdir(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", ".."))
import re,glob
files=glob.glob('core/src/**/*.java',recursive=True)+glob.glob('lwjgl3/src/**/*.java',recursive=True)+glob.glob('android/src/**/*.java',recursive=True)
changed=0
for f in files:
    s=open(f).read()
    m=re.search(r'^(package [\w.]+;\n)((?:\s*\n|import [^\n]+;\n)+)',s,re.M)
    if not m: continue
    block=m.group(2)
    imps=re.findall(r'^import [^\n]+;$',block,re.M)
    rest=s[m.end():]
    scan=re.sub(r'"(\\.|[^"\\])*"','""',rest)
    scan=re.sub(r'//.*','',scan)
    code=re.sub(r'/\*.*?\*/','',scan,flags=re.S)
    keep=[]
    for i in imps:
        name=i.rstrip(';').split('.')[-1]
        if name=='*' or i.startswith('import static'): keep.append(i); continue
        # used in code, or in javadoc {@link Name}
        if re.search(r'(?<![\w.])'+name+r'\b',code) or re.search(r'\{@link(?:plain)? '+name+r'\b',rest):
            keep.append(i)
    keep=sorted(set(keep))
    groups=[[i for i in keep if i.startswith('import static')],
            [i for i in keep if re.match(r'import (java|javax)\.',i)],
            [i for i in keep if i.startswith('import com.') or i.startswith('import org.') or i.startswith('import android')],
            [i for i in keep if i.startswith('import ohheck')]]
    other=[i for i in keep if not any(i in g for g in groups)]
    groups.append(other)
    newblock='\n'+'\n\n'.join('\n'.join(g) for g in groups if g)+'\n\n'
    if not keep: newblock='\n'
    n=s[:m.start(2)]+newblock+rest.lstrip('\n')
    if n!=s: open(f,'w').write(n); changed+=1
print('changed',changed)
