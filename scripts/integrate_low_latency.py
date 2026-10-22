from pathlib import Path
for name in ["app/src/main/java/com/bytbeat/mpctrainer/TrainerActivity.kt", "app/src/main/java/com/bytbeat/mpctrainer/TrainerApp.kt", "app/src/test/java/com/bytbeat/mpctrainer/TrainerBehaviorTest.kt"]:
 p=Path(name);p.write_text(p.read_text().replace("MPC УЧЁБА · 229", "MPC УЧЁБА · 230"))
print("Prewarmed low latency output; two device bursts; build 230")
