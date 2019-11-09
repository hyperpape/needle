![Badge](https://travis-ci.org/hyperpape/StringMatching.svg?branch=master)
![Badge](https://www.repostatus.org/badges/latest/wip.svg)

String searching/matching algorithms in Java. Where effective, the
library compiles those algorithms to bytecode to improve matching
speed.

This is an early experiment. Bytecode compiled regexes only support full
string matching, not searching. Most character classes and other
options are not yet implemented. There's not yet a reasonable public
API.

### Unicode

Currently limited to the BMP

### Performance

- the NFA class is often vastly slower than than Java regexes, though not always
- the DFA class is sometimes faster than Java regexes, but often slower
- byte compiled regexes are faster than Java regexes in many cases, but not all
- compilation performance is quite bad

### Building

Requires Java 11.

    mvn compile