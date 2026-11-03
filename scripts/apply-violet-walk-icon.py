from pathlib import Path
p=Path('app/src/main/assets/index.html')
s=p.read_text(encoding='utf-8')
old="if(/свободное время|отдых|отдохнуть|прогулка|погулять|кино|расслаб/.test(t)) return svg('<circle cx=\"12\" cy=\"12\" r=\"9\"/><path d=\"M12 7v5l3 2\"/>');"
new="if(/зарядка|разминка|прогулка|погулять/.test(t)) return svg('<circle cx=\"12\" cy=\"4.5\" r=\"2\"/><path d=\"M12 7v9M12 10 4 2M12 10 20 2M12 16 7 22M12 16 17 22\"/>');\n if(/свободное время|отдых|отдохнуть|кино|расслаб/.test(t)) return svg('<circle cx=\"12\" cy=\"12\" r=\"9\"/><path d=\"M12 7v5l3 2\"/>');"
if s.count(old)!=1:raise SystemExit('Walk icon insertion point not found')
p.write_text(s.replace(old,new,1),encoding='utf-8')
print('Installed exercise SVG with raised V arms for walk/charge tasks')
