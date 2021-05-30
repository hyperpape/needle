# Needle 

![Badge](https://travis-ci.com/hyperpape/StringMatching.svg?branch=master)
![Badge](https://www.repostatus.org/badges/latest/wip.svg)

String searching/matching algorithms in Java, including multi-string
matching and searching, and regular expressions.

The regular expression engine is
[non-backtracking](https://swtch.com/~rsc/regexp/regexp1.html), and
spends extra-effort at compile time to give better run-time matching
performance. Those compile time-efforts currently take two forms:

  1. Bytecode compilation: creating a specialized class for an
  individual regex that specializes the code to simulate an automaton,
  reducing interpretation overhead.
     
  2. Regular expressions with a literal prefix (e.g. `http://.*`) have specialized matching that seeks for a possible 
     prefix using an explicit for loop (skipping the automaton code). 
     
  3. The automaton can look ahead for necessary characters later in the stream. Upon seeing `h` in`http://.*`, the 
     automaton will check whether `/` is found 6 characters ahead, before moving to the next state.

### Status

This project has no users as of yet. 

### Syntax

Attempts to match the standard library syntax for all supported operations. Capturing groups and backreferences are not
supported. 

The following character classes are supported:

    \a, \d, \D, \e, \f, \h, \s, \S, \t, \w, \W, \x, \0, \., \\

### Unicode

The library supports searching against any string, however the needles that we search
for are currently limited to the BMP. 

### Performance

- byte compiled regexes are faster than Java regexes in many cases, but not all (benchmarks forthcoming)
- compilation performance is quite slow

### Building

Requires Java 11.

    mvn compile
