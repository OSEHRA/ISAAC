package sh.isaac.komet.batch.action;

import org.jvnet.hk2.annotations.Service;
import sh.isaac.api.observable.coordinate.ObservableManifoldCoordinate;

import jakarta.inject.Singleton;

import static sh.isaac.komet.batch.action.InactivateComponent.INACTIVATE_COMPONENT;


@Service
@Singleton
public class InactivateComponentFactory implements ActionFactory {

    @Override
    public String getActionName() {
        return INACTIVATE_COMPONENT;
    }

    @Override
    public ActionItem makeActionItem(ObservableManifoldCoordinate manifoldForDisplay) {
        InactivateComponent item = new InactivateComponent();
        item.setupForGui(manifoldForDisplay);
        return item;
    }
}

