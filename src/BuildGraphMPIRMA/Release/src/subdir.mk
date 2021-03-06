
# Add inputs and outputs from these tool invocations to the build variables 
CPP_SRCS += \
../../src/Dataset.cpp \
../../src/Edge.cpp \
../../src/HashTable.cpp \
../../src/OverlapGraph.cpp \
../../src/Read.cpp \
../../src/main.cpp 

OBJS += \
./src/Dataset.o \
./src/Edge.o \
./src/HashTable.o \
./src/OverlapGraph.o \
./src/Read.o \
./src/main.o 

CPP_DEPS += \
./src/Dataset.d \
./src/Edge.d \
./src/HashTable.d \
./src/OverlapGraph.d \
./src/Read.d \
./src/main.d 

ifeq ($(READGZ), 1)  #at this point, the makefile checks if FEATURE is enabled
OPTS = -DINCLUDE_READGZ #variable passed to g++
GZLIB = -lz 
endif

# Each subdirectory must supply rules for building sources it contributes
src/%.o: ../src/%.cpp
	@echo 'Building file: $<'
	@echo 'Invoking: GCC C++ Compiler'
	-$(CC) $(OPTS) -g3 -Wall -c -fmessage-length=0 -fopenmp -Wno-sign-compare $(GZLIB) -std=c++11 -O3 -lgomp -MMD -MP -MF"$(@:%.o=%.d)" -MT"$(@)" -o "$@" "$<"
	@echo 'Finished building: $<'
	@echo ' '


