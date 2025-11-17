## Clone the zxing-cpp project repository

```bash
git clone https://github.com/zxing-cpp/zxing-cpp.git  
```

## For macos :

 - Navigate to the zxing-cpp directory
 - Build the c++ project : 

```bash
cmake -B build -S . -DCMAKE_BUILD_TYPE=Release -DBUILD_SHARED_LIBS=ON -DBUILD_EXAMPLES=OFF -DBUILD_TESTING=OFF
cmake --build build --config Release
cmake --install build --prefix ../local
```

 -  Now build the jna wrapper :

``` bash
c++ -std=c++17 -O3 -shared -fPIC zxing_wrapper.cpp -o libzxing_jna.dylib \
  -I local/include -L local/lib -lZXing \
  -Wl,-rpath,@loader_path 
```

You should now have the `libzxing_jna.dylib` file in the zxing-cpp directory.

 - You will need to copy this file and the library system file to your Java project

```bash
# To identify the library system file needed by the wrapper use this command : 
otool -L libzxing_jna.dylib
# This file should be located in the local/lib folder
```

Finally, copy this file and the library system file to the `src/main/resources` directory of the common-library project inside the folder : 
natives.macos-aarch64

## How to build for linux :

- Use the dockerfile with this commands :

```bash
docker build --platform linux/amd64 -t zxing-linux-dyn .

# crée un conteneur "stopé" en amd64, copie /dist, puis supprime
cid=$(docker create --platform linux/amd64 zxing-linux-dyn)
docker cp "$cid":/dist ./dist
docker rm "$cid"
```

Finally, copy the files from the dist folder to the `src/main/resources` directory of the common-library project inside the folder :
natives.linux-x86_64


rm -rf build_lib
cmake -B build_lib -S .
cmake --build build_lib --config Release