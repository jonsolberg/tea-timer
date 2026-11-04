package se.jonsolberg.teatimer;

/** A kind of tea with its default steep time and water temperature. */
final class Tea {
    final String id;
    final int nameRes;
    final int defaultSeconds;
    final String temp;

    private Tea(String id, int nameRes, int defaultSeconds, int celsius) {
        this.id = id;
        this.nameRes = nameRes;
        this.defaultSeconds = defaultSeconds;
        this.temp = celsius > 0 ? celsius + " °C" : "";
    }

    boolean isCustom() {
        return "custom".equals(id);
    }

    static final Tea[] ALL = {
            new Tea("white", R.string.tea_white, 4 * 60, 80),
            new Tea("green", R.string.tea_green, 2 * 60 + 30, 80),
            new Tea("sencha", R.string.tea_sencha, 90, 70),
            new Tea("jasmine", R.string.tea_jasmine, 3 * 60, 80),
            new Tea("oolong", R.string.tea_oolong, 3 * 60 + 30, 90),
            new Tea("black", R.string.tea_black, 4 * 60, 95),
            new Tea("earl_grey", R.string.tea_earl_grey, 4 * 60, 95),
            new Tea("darjeeling", R.string.tea_darjeeling, 3 * 60, 90),
            new Tea("puerh", R.string.tea_puerh, 4 * 60, 95),
            new Tea("rooibos", R.string.tea_rooibos, 6 * 60, 100),
            new Tea("herbal", R.string.tea_herbal, 6 * 60, 100),
            new Tea("custom", R.string.tea_custom, 3 * 60, 0),
    };

    static Tea byId(String id) {
        for (Tea t : ALL) {
            if (t.id.equals(id)) return t;
        }
        return ALL[5]; // black tea
    }
}
