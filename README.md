## Build Instructions
In the terminal, run `$ mvn package` to build the project.

On Linux, run `$ java -cp lib/com.microsoft.z3.jar:target/verif-comp-1.0.jar smt.Main <input>` to run the executable jar.

On Windows, run `$ java -cp "lib/com.microsoft.z3.jar;target/verif-comp-1.0.jar" smt.Main <input>` to run the executable jar.

`<input>` is the (relative) path to the program to be interpreted.

Update the build instructions if any external libraries are added.
