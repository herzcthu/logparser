package nl.basjes.parse.core.reference;

import nl.basjes.parse.core.Casts;
import nl.basjes.parse.core.Dissector;
import nl.basjes.parse.core.Parsable;
import nl.basjes.parse.core.SimpleDissector;
import nl.basjes.parse.core.exceptions.DissectionFailure;
import nl.basjes.parse.core.exceptions.InvalidDissectorException;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;

public class FooDissector extends SimpleDissector {

    private static HashMap<String, EnumSet<Casts>> dissectorConfig = new HashMap<>();
    static {
        dissectorConfig.put("ANY:fooany",         Casts.STRING_OR_LONG_OR_DOUBLE);
        dissectorConfig.put("STRING:foostring",   Casts.STRING_ONLY);
        dissectorConfig.put("INT:fooint",         Casts.STRING_OR_LONG);
        dissectorConfig.put("LONG:foolong",       Casts.STRING_OR_LONG);
        dissectorConfig.put("FLOAT:foofloat",     Casts.STRING_OR_DOUBLE);
        dissectorConfig.put("DOUBLE:foodouble",   Casts.STRING_OR_DOUBLE);
    }

    public FooDissector() {
        super("FOOINPUT", dissectorConfig);
    }

    @Override
    public void dissect(Parsable<?> parsable, String inputname) throws DissectionFailure {
        parsable.addDissection(inputname, "ANY",    "fooany",    "42");
        parsable.addDissection(inputname, "STRING", "foostring", "42");
        parsable.addDissection(inputname, "INT",    "fooint",    42);
        parsable.addDissection(inputname, "LONG",   "foolong",   42L);
        parsable.addDissection(inputname, "FLOAT",  "foofloat",  42F);
        parsable.addDissection(inputname, "DOUBLE", "foodouble", 42D);
    }
}
