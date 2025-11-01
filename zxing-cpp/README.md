# In this README we will explain how to build the zxing-cpp wrapper for DossierFacile on macos and linux

## For you local machine :

⚠️⚠️ : Make sur your machine have the correct version of GCC and G++ installed (12 or nothing) if you don't have the library will be compiled with the legacy mode that will not detect DataMatrix correctly !

 - Go inside data folder and clone the zxing-cpp repository :

```bash
  git clone https://github.com/zxing-cpp/zxing-cpp.git
```
 - Now create the cmake directory : 
```bash
    cmake -B build_lib -S .
```
 - Build the c++ library :
```bash
    cmake --build build_lib --config Release
```
 - Now inside the zxing_build directory you have the compiled jna wrapper that include the zxing-cpp library you have to move it to the corresponding jna directory :
   - For macos : copy the `libzxing_jna.dylib` file to the `src/main/resources/natives.macos-aarch64` directory of the common-library project
   - For linux : copy the `libzxing_jna.so` file to the `src/main/resources/natives.linux-x86_64` directory of the common-library project

## To cross compile with docker : 
- You have to start the container with the docker-compose file : 
```bash
  docker-compose up -d --no-deps --build
```

- Now you have to access the container bash : 
```bash
  docker exec -it zxing-cpp-vm /bin/bash 
```

- Now you can follow the same steps as for the local machine to build the zxing-cpp library and copy the compiled jna wrapper to the corresponding jna directory in the common-library project.
