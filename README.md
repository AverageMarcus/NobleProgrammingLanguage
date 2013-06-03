# Required

* Antlr - <http://www.antlr.org/>
* LLVM - <http://llvm.org/>

# Building the antlr grammar

* antlr4 -no-listener -visitor Noble.g4

# Compiling a program

* javac *.java && java NC {FileName}.ll {FileName}.nbl && llvm-as {FileName}.ll && llvm-ld -o {FileName} {FileName}.bc

# Running the compiled program

* ./{FileName}

## Example programs

### Hello World - Traditional "Hello World" program
 1. Build:    javac *.java && java NC hello_world.ll Examples/hello_world.nbl && llvm-as hello_world.ll && llvm-ld -o hello_world hello_world.bc
 2. Run:	./hello_world

### Calculate Rectangle Area - Returns the area of two inputted integers
 1. Build:	javac *.java && java NC rectangle_area.ll Examples/rectangle_area.nbl && llvm-as rectangle_area.ll && llvm-ld -o rectangle_area rectangle_area.bc
 2. Run:	./rectangle_area

### Factorial - Returns the factorial of a given number
 1. Build:	javac *.java && java NC factorial.ll Examples/factorial.nbl && llvm-as factorial.ll && llvm-ld -o factorial factorial.bc
 2. Run:	./factorial

### Echo - Repeats back what the user types in
 1. Build:	javac *.java && java NC echo.ll Examples/echo.nbl && llvm-as echo.ll && llvm-ld -o echo echo.bc
 2. Run:	./echo

### Menu - Provides a menu with 3 options and an exit choice. There's also Pie!
 1. Build:	javac *.java && java NC menu.ll Examples/menu.nbl && llvm-as menu.ll && llvm-ld -o menu menu.bc
 2. Run:	./menu

### Inception - Demonstrates nested function calls
 1. Build:	javac *.java && java NC inception.ll Examples/inception.nbl && llvm-as inception.ll && llvm-ld -o inception inception.bc
 2. Run:	./inception

### Isosceles Triangle - Prints out an isosceles triangle of stars
 1. Build:	javac *.java && java NC isosceles_triangle.ll Examples/isosceles_triangle.nbl && llvm-as isosceles_triangle.ll && llvm-ld -o isosceles_triangle isosceles_triangle.bc
 2. Run:	./isosceles_triangle

### Fibonacci sequence - Returns the Fibonacci sequence for the provided number
 1. Build:	javac *.java && java NC fibonacci.ll Examples/fibonacci.nbl && llvm-as fibonacci.ll && llvm-ld -o fibonacci fibonacci.bc
 2. Run:	./fibonacci

### BubbleSort - Sorts a list of 1000 integers
 1. Build:	javac *.java && java NC bubble_sort.ll Examples/bubble_sort.nbl && llvm-as bubble_sort.ll && llvm-ld -o bubble_sort bubble_sort.bc
 2. Run:	./bubble_sort

