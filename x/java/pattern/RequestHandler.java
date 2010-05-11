
package x.java.pattern;


public interface RequestHandler<I, E>
{
    E handleRequest(I _if);
}
