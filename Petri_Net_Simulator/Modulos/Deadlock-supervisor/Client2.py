import os
import shutil
pathActual = os.path.dirname(os.path.realpath(__file__))
print(pathActual)
files = os.listdir(pathActual)
for name in files:
    if name.endswith(".txt"):
            os.remove(pathActual+"/"+name)
shutil.rmtree(pathActual+"/"+"__pycache__")