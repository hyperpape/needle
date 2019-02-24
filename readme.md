Compiling string matching algorithms directly to Java bytecodes.

Includes an NFA/DFA interpreter, a regex -> NFA parser, an NFA -> DFA
compiler, and a DFA -> bytecode compiler. Unicode support is currently limited
to the BMP.

Very very early experiment.

### Performance

In principle generating classes specialized for a particular automaton
should be capable of outperforming Java regular expressions, which are
not specialized.

In practice, I have done minimal performance testing. The string
algorithms in Search perform worse than indexOf, which is a hotspot
intrinsic.

### Building

Requires Java 11.

    mvn compile