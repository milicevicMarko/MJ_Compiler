# MJ Compiler
 
 This project was an assignment for the class : **Compiler Construction 1** (srb. *Programski Prevodioci 1*)
 
 The assignment and all the rules can be found in the assignment folder [here](https://github.com/milicevicMarko/MJ_Compiler/tree/master/assignment).
 
 ## Phases:
 ### 1. Lexical analysis
 ### 2. Parsing
 ### 3. Semantic analysis  
 ### 4. Code generating
 
 ## todo improve readme by explaining every step

## Usage

> General advice is to use Intellij as it makes our lives simpler :) 

##### Compile

The file to be compiled should be placed in the [test](https://github.com/milicevicMarko/MJ_Compiler/blob/master/test) 

The extension of the file to be compiled should be **.mj**

The file to be used as input valued is **program.in**

To use the compiler, go to [test/rs/ac/bg/etf/pp1/compiler](https://github.com/milicevicMarko/MJ_Compiler/blob/master/test/rs/ac/bg/etf/pp1/Compiler.java) 

Set the name of the file to be compiled as a VM arguments (eg. _program.mj_)

##### Run

To run the program, go to [build.xml](https://github.com/milicevicMarko/MJ_Compiler/blob/master/build.xml), and run the **runObj** target  

##### Debug

To debug the program, remove the comment symbols in the [build.xml](https://github.com/milicevicMarko/MJ_Compiler/blob/master/build.xml), on line **74**.