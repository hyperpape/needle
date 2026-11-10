# Architecture and Optimizations

This document is a short description of the optimizations in needle. It presupposes familiarity with DFA based regex
engines.

## Overview

- standard parse -> NFA -> DFA pipeline
- hopcroft minimization of regex states
- eager compilation
- each pattern generates one pattern class, one matcher class
- matcher class can be run using `needle-types` library, compiler is not needed at runtime

## Multiple DFAs

The pattern class is built from a selection of DFAs:

- one for full string match
- one for reporting whether a string contains the needle at any point
- one for finding the end of an un-anchored match
- one for backwards search from the end of an un-anchored match to find the beginning
- one for suffix driven searches

DFA states are represented using `byte` for small DFAs, and `short` for larger ones.

## Abstract Alphabet

- matchers operate on java characters
- maps characters to bytes for state transition arrays to minimize transition array size
- mapping is minimal (all equivalent characters map to the same byte)
- number of byte-classes is rounded up to power of 2 when <= 16 so JIT compiler can treat it as a shift
  `stateTransitions[state << n]`
- regexes with >= 255 byte-classes use a less-optimized approach that requires rework
- share byte-class mappings where possible, otherwise one-per DFA

## Full String Match

- prefilter by length
    - min length
    - max length
    - filter based on lengths the regex can have:
        - exclude when `(length - constant) % gcd != 0 ` (gcd of regex component sizes)
        - numerical semigroup based filtering
- optimize self-transitioning final state (e.g. `.*foo.*` or `[a-z]*`)
    - fast loop where needed (e.g. until end of line)
        - fast loop can use 1-2 character match, or `boolean[]` lookup on chars
    - immediately return true (when final loop can accept any character including `\n`)

## Contained In

- from starting state, prefilter using distance to end of string  
- where possible, searches use String#indexOf() to find the prefix

## Index Forwards

- from starting state, prefilter using distance to end of string 
- where possible, searches begin by using String#indexOf to find
    - a prefix (first choice)
    - a suffix
        - requires the string to have a maximum length 
        - state based matching starts at earliest point that could reach that suffix
    - an infix search
        - requires the string have a maximum length
        - state based matching starts at earliest point that could reach that infix
- when no affix is usable searches can begin by scanning for the first character
    - by lookup in a `boolean[]`
        - character frequencies determine whether first-character scan can be used
        - if the first state self-loops (e.g. `[a-z]*[0-9]+`), this scan searches for a character leaving the initial
          state
    - by direct comparison for 1-2 characters
- optimize self-transitioning final state (e.g. `.*foo.*` or `[a-z]*`)
    - fast loop where needed (e.g. search until end of line)
        - fast loop can use 1-2 character match, or `boolean[]` lookup on chars
    - immediately return end of string when final loop can accept any character including `\n`

## Index Backwards

- backwards DFA is implicitly anchored from endpoint--runs until dead then returns best match
- skip full DFA search where possible
    - search backwards for starting character when it cannot occur in middle of needle (e.g. `SC-[0-9]+`).
    - calculate starting index when the regex has only one possible length
- skips backwards over mandatory suffix when it exists
- suffix/prefix match from forwards search bounds the earliest index backwards dfa can consider

## Suffix Search

- use backwards searches from suffix where possible
    - suffixes that cannot occur except in the last position of the needle can do a backwards-only match
    - backwards searches that are guaranteed to find the earliest possible starting point do a backwards search then a
      forwards

## Accepting States

- checks for accepting states use direct comparison for a single value or `boolean[]` lookup for larger sets
