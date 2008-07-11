// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.syntax.json;

import java.io.EOFException;
import java.io.Reader;

public final class
JSONParser {
    
    static public interface
    Builder {
        
        static public interface
        ObjectBuilder {
            void finish() throws Exception;
            Builder startMember(String name) throws Exception;
        }
        ObjectBuilder startObject() throws Exception;
        
        static public interface
        ArrayBuilder {
            void finish() throws Exception; 
            Builder startElement() throws Exception;
        }
        ArrayBuilder startArray() throws Exception;
          
        void writeKeyword(String token) throws Exception;
        void writeString(String token) throws Exception;
    }

    static private final String whitespace = " \n\r\t";
    
    static private void
    eatWhitespace(final char c) throws Exception {
        if (whitespace.indexOf(c) == -1) {
            throw new Exception("0x" + Integer.toHexString(c));
        }
    }
    
    static private interface
    State {
        void run(char c) throws Exception;
    }
    
    static private final class
    Stack {
        private State[] states;
        private int top;
        
        protected
        Stack() {
            states = new State[16];
            top = -1;
        }
        
        protected boolean
        isEmpty() { return top == -1; }
        
        protected State
        peek() { return states[top]; }
        
        protected void
        pop() { states[top--] = null; }
        
        protected void
        push(final State child) {
            ++top;
            if (states.length == top) {
                System.arraycopy(states, 0, states = new State[2*top], 0, top);
            }
            states[top] = child;
        }
        
        protected void
        swap(final State next) { states[top] = next; }
    }

    private final Stack state = new Stack();
    
    private
    JSONParser() {}
    
    static public void
    drive(final String id, final Reader in, final Builder out) throws Exception{
        final JSONParser parser = new JSONParser();
        parser.state.push(new State() {
            public void
            run(final char c) throws Exception { eatWhitespace(c); }
        });
        parser.state.push(parser.parseValue(out));
        int line = 1;
        int column = 1;
        try {
            for (int i = in.read(); -1 != i; i = in.read()) {
                parser.state.peek().run((char)i);
                if ('\n' == i) {
                    ++line;
                    column = 1;
                } else {
                    ++column;
                }
            }
            parser.state.pop();
            if (!parser.state.isEmpty()) { throw new EOFException(); }
        } catch (final Exception e) {
            try { in.close(); } catch (final Exception e2) {}
            throw new Exception("<"+id+"> ( "+line+ ", "+column+" ) : ", e);
        }
        in.close();
    }
    
    private State
    parseValue(final Builder out) {
        return new State() {
            public void
            run(final char c) throws Exception {
                switch (c) {
                case '\"':
                    state.swap(parseString(new StringReceiver() {
                        public void
                        run(final String token) throws Exception {
                            out.writeString(token);
                        }
                    }));
                    break;
                case '{':
                    state.swap(parseFirstMember(out.startObject()));
                    break;
                case '[':
                    state.swap(parseFirstElement(out.startArray()));
                    break;
                default:
                    if (whitespace.indexOf(c) == -1) {
                        state.swap(parseKeyword(new StringReceiver() {
                            public void
                            run(final String token) throws Exception {
                                out.writeKeyword(token);
                            }
                        }));
                        state.peek().run(c);
                    }
                 }
            }
        };
    }
    
    private State
    parseFirstMember(final Builder.ObjectBuilder out) {
        return new State() {
            public void
            run(final char c) throws Exception {
                if ('}' == c) {
                    state.pop();
                    out.finish();
                } else {
                    if (whitespace.indexOf(c) == -1) {
                        state.swap(parseNextMember(out));
                        state.push(parseMember(out));
                        state.peek().run(c);
                    }
                }
            }
            
        };
    }
    
    private State
    parseNextMember(final Builder.ObjectBuilder out) {
        return new State() {
            public void
            run(final char c) throws Exception {
                switch (c) {
                case ',':
                    state.push(parseMember(out));
                    break;
                case '}':
                    state.pop();
                    out.finish();
                    break;
                default:
                    eatWhitespace(c);
                }
            }
        };
    }
    
    private State
    parseMember(final Builder.ObjectBuilder out) {
        final StringReceiver startMember = new StringReceiver() {
            public void
            run(final String name) throws Exception {
                state.push(parseMemberValue(out.startMember(name)));
            }
        };
        return new State() {            
            public void
            run(final char c) throws Exception {
                switch (c) {
                case '\"':
                    state.swap(parseString(startMember));
                    break;
                default:
                    if (whitespace.indexOf(c) == -1) {
                        state.swap(parseKeyword(startMember));
                    }
                }
            }
        };
    }
    
    private State
    parseMemberValue(final Builder out) {
        return new State() {
            public void
            run(final char c) throws Exception {
                if (':' == c) {
                    state.swap(parseValue(out));
                } else {
                    eatWhitespace(c);
                }
            }
        };
    }
    
    private State
    parseFirstElement(final Builder.ArrayBuilder out) {
        return new State() {
            public void
            run(final char c) throws Exception {
                if (']' == c) {
                    state.pop();
                    out.finish();
                } else {
                    if (whitespace.indexOf(c) == -1) {
                        state.swap(parseNextElement(out));
                        state.push(parseValue(out.startElement()));
                        state.peek().run(c);
                    }
                }
            }
            
        };
    }
    
    private State
    parseNextElement(final Builder.ArrayBuilder out) {
        return new State() {
            public void
            run(final char c) throws Exception {
                switch (c) {
                case ',':
                    state.push(parseValue(out.startElement()));
                    break;
                case ']':
                    state.pop();
                    out.finish();
                    break;
                default:
                    eatWhitespace(c);
                }
            }
        };
    }
    
    static private interface
    StringReceiver {
        void run(String value) throws Exception;
    }
    
    private State
    parseKeyword(final StringReceiver out) {
        final StringBuilder buffer = new StringBuilder();
        return new State() {
            public void
            run(final char c) throws Exception {
                if (whitespace.indexOf(c) == -1 && ":,}]".indexOf(c) == -1) {
                    buffer.append(c);
                } else {
                    state.pop();
                    out.run(buffer.toString());
                    state.peek().run(c);
                }
            }
        };
    }
    
    private State
    parseString(final StringReceiver out) {
        final StringBuilder buffer = new StringBuilder();
        return new State() {
            public void
            run(final char c) throws Exception {
                if ('\"' == c) {
                    state.pop();
                    out.run(buffer.toString());
                } else if ('\\' == c) {
                    state.push(parseEscape(buffer));
                } else {
                    buffer.append(c);
                }
            }
        };
    }
    
    private State
    parseEscape(final StringBuilder out) {
        return new State() {
            public void
            run(final char c) throws Exception {
                switch (c) {
                case '\"':
                    out.append('\"');
                    state.pop();
                    break;
                case '\\':
                    out.append('\\');
                    state.pop();
                    break;
                case '/':
                    out.append('/');
                    state.pop();
                    break;
                case 'b':
                    out.append('\b');
                    state.pop();
                    break;
                case 'f':
                    out.append('\f');
                    state.pop();
                    break;
                case 'n':
                    out.append('\n');
                    state.pop();
                    break;
                case 'r':
                    out.append('\r');
                    state.pop();
                    break;
                case 't':
                    out.append('\t');
                    state.pop();
                    break;
                case 'u':
                    state.swap(parseUnicode(out));
                    break;
                default:
                    throw new Exception("0x" + Integer.toHexString(c));
                }
            }            
        };
    }
    
    private State
    parseUnicode(final StringBuilder out) {
        return new State() {
            private int unicode = 0;
            private int expected = 4;
            
            public void
            run(final char c) throws Exception {
                unicode <<= 4;
                if ('0' <= c && '9' >= c) {
                    unicode |= (c - '0') & 0x0F;
                } else if ('A' <= c && 'F' >= c) {
                    unicode |= (c - 'A' + 10) & 0x0F;
                } else if ('a' <= c && 'f' >= c) {
                    unicode |= (c - 'a' + 10) & 0x0F;
                } else {
                    throw new Exception("0x" + Integer.toHexString(c));
                }
                if (--expected == 0) {
                    out.append((char)unicode);
                    state.pop();
                }
            }
        };
    }
}