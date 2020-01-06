![Badge](https://travis-ci.org/hyperpape/StringMatching.svg?branch=master)
![Badge](https://www.repostatus.org/badges/latest/wip.svg)

String searching/matching algorithms in Java, including multi-string
matching and searching, and regular expressions.

The regular expression engine is
[non-backtracking](https://swtch.com/~rsc/regexp/regexp1.html), and
spends extra-effort at compile time to give better run-time matching
performance. Those compile time-efforts currently take three forms:

  1. Recognizing limited regular expressions that can be matched with
  simpler techniques (Aho-Corasick, for instance).

  2. State minimization

  3. Bytecode compilation: creating a specialized class for an
  individual regex that specializes the code to simulate an automaton,
  reducing interpretation overhead.

### Status

This is an early experiment. Bytecode compiled regexes only support full
string matching, not searching. Most character classes and other
options are not yet implemented. There's not yet a reasonable public
API.

### Unicode

We can search against any string, however the needles that we search
for are currently limited to the BMP. 

### Performance

- the NFA class is often vastly slower than than Java regexes, though not always
- the DFA class is sometimes faster than Java regexes, but often slower
- byte compiled regexes are faster than Java regexes in many cases, but not all
- compilation performance is quite bad

### Building

Requires Java 11.

    mvn compile