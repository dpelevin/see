package see.functions;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * Alias to shorten declaration length.
 * @param <Arg> type of arguments
 * @param <Result> result type
 */
public interface VarArgFunction<Arg, Result> extends Function<List<Arg>, Result> {
    @Override
    Result apply(@Nonnull List<Arg> args);
}
