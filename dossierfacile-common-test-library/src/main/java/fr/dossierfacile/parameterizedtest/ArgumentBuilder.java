package fr.dossierfacile.parameterizedtest;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.params.provider.Arguments;

import java.util.Arrays;
import java.util.List;

public class ArgumentBuilder {

    private ArgumentBuilder(){}

    public static <T> Arguments buildArguments(String name, ControllerParameter<T> object) {
        return Arguments.of(Named.of(name, object));
    }

    @SafeVarargs
    public static <T> List<Arguments> buildListOfArguments(Pair<String, ControllerParameter<T>>... pairList) {
        return Arrays.stream(pairList).map(pair -> buildArguments(pair.getLeft(), pair.getRight())).toList();
    }
}
