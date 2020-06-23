/* 
 * Licensed under the Apache License, Version 2.0 (the "License");
 *
 * You may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributions from 2013-2017 where performed either by US government 
 * employees, or under US Veterans Health Administration contracts. 
 *
 * US Veterans Health Administration contributions by government employees
 * are work of the U.S. Government and are not subject to copyright
 * protection in the United States. Portions contributed by government 
 * employees are USGovWork (17USC §105). Not subject to copyright. 
 * 
 * Contribution by contractors to the US Veterans Health Administration
 * during this period are contractually contributed under the
 * Apache License, Version 2.0.
 *
 * See: https://www.usa.gov/government-works
 * 
 * Contributions prior to 2013:
 *
 * Copyright (C) International Health Terminology Standards Development Organisation.
 * Licensed under the Apache License, Version 2.0.
 *
 */



package sh.isaac.model.observable.coordinate;

//~--- JDK imports ------------------------------------------------------------

import javafx.beans.property.SimpleObjectProperty;
import sh.isaac.api.coordinate.*;
import sh.isaac.model.observable.ObservableFields;

//~--- non-JDK imports --------------------------------------------------------

//~--- classes ----------------------------------------------------------------

/**
 * The Class ObservableManifoldCoordinateImpl.
 *
 * @author kec
 */
public class ObservableManifoldCoordinateImpl extends ObservableManifoldCoordinateBase {

    public ObservableManifoldCoordinateImpl(ManifoldCoordinate manifoldCoordinate, String name) {
        super(manifoldCoordinate, name);
    }

    public ObservableManifoldCoordinateImpl(ManifoldCoordinate manifoldCoordinate) {
        super(manifoldCoordinate);
    }

    @Override
    protected ObservableNavigationCoordinateImpl makeNavigationCoordinateProperty(ManifoldCoordinate manifoldCoordinate) {
        return new ObservableNavigationCoordinateImpl(manifoldCoordinate.toNavigationCoordinateImmutable());
    }

    @Override
    protected ObservableStampFilterBase makeLanguageStampFilterProperty(ManifoldCoordinate manifoldCoordinate) {
        return ObservableStampFilterImpl.make(manifoldCoordinate.getLanguageStampFilter(), ObservableFields.LANGUAGE_FILTER_FOR_DIGRAPH.toExternalString());
    }

    @Override
    protected ObservableStampFilterBase makeVertexStampFilterProperty(ManifoldCoordinate manifoldCoordinate) {
        return  ObservableStampFilterImpl.make(manifoldCoordinate.getVertexStampFilter(), ObservableFields.VERTEX_FILTER_FOR_DIGRAPH.toExternalString());
    }

    @Override
    protected ObservableStampFilterBase makeEdgeStampFilterProperty(ManifoldCoordinate manifoldCoordinate) {
        return ObservableStampFilterImpl.make(manifoldCoordinate.getEdgeStampFilter(), ObservableFields.EDGE_FILTER_FOR_DIGRAPH.toExternalString());
    }

    @Override
    protected SimpleObjectProperty<VertexSort> makeVertexSortProperty(ManifoldCoordinate manifoldCoordinate) {
        return new SimpleObjectProperty<>(this,
                ObservableFields.VERTEX_SORT_PROPERTY.toExternalString(),
                manifoldCoordinate.getVertexSort());
    }


    @Override
    protected ObservableLanguageCoordinateBase makeLanguageCoordinate(ManifoldCoordinate manifoldCoordinate) {
        return new ObservableLanguageCoordinateImpl(manifoldCoordinate.getLanguageCoordinate());
    }

}


