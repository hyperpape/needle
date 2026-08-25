# Needle

This is a high performance regular expression library that aims to trade off compile-time effort on analysis for faster runtime matching. 

The current direction is working on precompiling DFAs to Java classes, but the library isn't limited to that. The north star is that for as many regular expressions as possible, this library should implement a solution as fast as a knowledgable developer could hand implement on the JVM. However, we should also aim for general purpose solutions. 

## Engines

The project has three engines: 
1. NFA 
2. DFA (currently inefficient, does not implement Pattern, used to support compilation)
3. Bytecode compiled DFAs produced by DFACompiler.compile 

The third is the most important--any supported semantics must be correctly and efficiently implemented in it.   

## Compatibility 

This project aims for exact compatibility with the JDK, except for documented mismatches. 

Documented mismatches can only take the form of: 
1. Extra behavior unsupported by the standard library (e.g. optional leftmost-longest semantics), or syntax extensions (none so far).
2. Unsupported syntax, which should throw a compile-time exception in needle.  

## Testing and Correctness

Correctness of the code is essential. It is better to throw an exception than to return a wrong result. Never use default values that will give incorrect results.  

In general, the most effective tests we have follow the pattern of matches.txt--a specification of how a regular expression should match, which is then checked against the JDK standard library and needle, with as much detail as possible. Unit tests are primarily useful for making it easier to debug and isolate specific failures. 

These tests should monotonically increase their coverage. Tests should never fail on a commit that is part of origin/main. 

## Mako

[Mako](https://github.com/hyperpape/mako) is a dependency which we own, it can be improved or altered for the sake of this project. 

## Running Tests

It's wise to add a timeout flag to all tests, as incorrect search logic often results in an infinite loop. 

Always run tests from the root directory with a full build--building is quick. 

When intentionally changing bytecode, we need to pass maven flags to run SnapshotTest#recordExamples. Normally we should not pass these flags, so that we catch inadvertent changes to compiled output for the snapshots. 

```
mvn test -Dneedle.recordSnapshots=true
```

## Comments and Commit Messages

Keep commits and comments free of superfluous content. Do not editorialize, a bug is a bug. 

Only mention design principles (e.g. JDK compatibility) where they explain unclear or surprising choices. For instance, a commit message for a parser change need not mention JDK compatibility. 

Describing the prior code behavior in a commit message is unnecessary, unless it is not obvious what the commit changed.  
