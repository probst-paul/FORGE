package forge.query;

import java.util.List;

public class FacadeForgeQuery {
    private static final FacadeForgeQuery THE_INSTANCE = new FacadeForgeQuery();

    private final QueryService queryService;
    private final ForgeQueryAccess access = new ForgeQueryAccess();

    public static FacadeForgeQuery getTheInstance() {
        return THE_INSTANCE;
    }

    public FacadeForgeQuery() {
        this(new QueryService());
    }

    public FacadeForgeQuery(QueryService queryService) {
        if (queryService == null) {
            throw new IllegalArgumentException("queryService is required");
        }
        this.queryService = queryService;
    }

    public ForgeQueryAccess forgeQueryAccess() {
        return access;
    }

    public class ForgeQueryAccess {
        public List<String> getSupportedQueryEventNames() {
            return queryService.getSupportedQueryEventNames();
        }
    }
}
