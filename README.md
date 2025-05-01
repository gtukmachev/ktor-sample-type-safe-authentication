# ktor-sample-type-safe-authentication

This projects illustrates the problem, described in StackOverflow question:

## See the code: 

See the kotlin/Application.kt file:
* :white_check_mark: the route `/private` works fine;
* :x: the route  `/private-2` has the error - **principal** is always null.
 

## Run test to see the problem:
```shell
./gradlew test --info
```