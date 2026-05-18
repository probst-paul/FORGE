package forge.stop;

import java.lang.reflect.InvocationTargetException;
import java.time.LocalTime;
import java.util.List;

public class FacadeForgeStop {
    private static final FacadeForgeStop THE_INSTANCE = new FacadeForgeStop();

    private final StopModelCatalog stopModelCatalog;
    private final ForgeStopAccess access = new ForgeStopAccess();

    public static FacadeForgeStop getTheInstance() {
        return THE_INSTANCE;
    }

    public FacadeForgeStop() {
        this(new StopModelCatalog());
    }

    public FacadeForgeStop(StopModelCatalog stopModelCatalog) {
        this.stopModelCatalog = stopModelCatalog;
    }

    public ForgeStopAccess forgeStopAccess() {
        return access;
    }

    public class ForgeStopAccess {
        public List<Class<? extends StopModel>> findAvailableStopModels() {
            return stopModelCatalog.findAvailableStopModels();
        }

        public String getDisplayName(Class<? extends StopModel> stopModel) {
            return stopModelCatalog.getDisplayName(stopModel);
        }

        public PriceBasedStop createPriceBasedStop(long stopPriceTicks) {
            return new PriceBasedStop(stopPriceTicks);
        }

        public TimeBasedStop createTimeBasedStop(LocalTime exitTime) {
            return new TimeBasedStop(exitTime);
        }

        public StopModel createStopModel(Class<? extends StopModel> stopModel) {
            try {
                return stopModel.getDeclaredConstructor().newInstance();
            } catch (InstantiationException
                     | IllegalAccessException
                     | InvocationTargetException
                     | NoSuchMethodException e) {
                throw new IllegalStateException("Unable to create stop model " + stopModel.getSimpleName(), e);
            }
        }
    }
}
