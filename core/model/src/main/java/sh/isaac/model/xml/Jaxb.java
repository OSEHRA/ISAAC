/*
 * Copyright 2018 Organizations participating in ISAAC, ISAAC's KOMET, and SOLOR development include the
         US Veterans Health Administration, OSHERA, and the Health Services Platform Consortium..
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package sh.isaac.model.xml;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import sh.isaac.api.ConceptProxy;
import sh.isaac.api.component.concept.ConceptSpecification;
import sh.isaac.api.query.And;
import sh.isaac.api.query.AndNot;
import sh.isaac.api.query.AttributeReturnSpecification;
import sh.isaac.api.query.Clause;
import sh.isaac.api.query.LeafClause;
import sh.isaac.api.query.LetItemKey;
import sh.isaac.api.query.Not;
import sh.isaac.api.query.Or;
import sh.isaac.api.query.Query;
import sh.isaac.api.query.Xor;
import sh.isaac.api.query.clauses.AssemblageContainsConcept;
import sh.isaac.api.query.clauses.AssemblageContainsKindOfConcept;
import sh.isaac.api.query.clauses.AssemblageContainsString;
import sh.isaac.api.query.clauses.AssemblageLuceneMatch;
import sh.isaac.api.query.clauses.ChangedBetweenVersions;
import sh.isaac.api.query.clauses.ComponentIsActive;
import sh.isaac.api.query.clauses.ConceptForComponent;
import sh.isaac.api.query.clauses.ConceptIs;
import sh.isaac.api.query.clauses.ConceptIsChildOf;
import sh.isaac.api.query.clauses.ConceptIsDescendentOf;
import sh.isaac.api.query.clauses.ConceptIsKindOf;
import sh.isaac.api.query.clauses.DescriptionActiveLuceneMatch;
import sh.isaac.api.query.clauses.DescriptionActiveRegexMatch;
import sh.isaac.api.query.clauses.DescriptionLuceneMatch;
import sh.isaac.api.query.clauses.DescriptionRegexMatch;
import sh.isaac.api.query.clauses.FullyQualifiedNameForConcept;
import sh.isaac.api.query.clauses.PreferredNameForConcept;
import sh.isaac.api.query.clauses.RelRestriction;
import sh.isaac.api.query.clauses.RelationshipIsCircular;
import sh.isaac.api.xml.JaxbMap;
import sh.isaac.model.coordinate.LanguageCoordinateImpl;
import sh.isaac.model.coordinate.StampCoordinateImpl;

/**
 *
 * @author kec
 */
public class Jaxb {
    private static final Jaxb jaxb = new Jaxb();
    final JAXBContext jc;
    private Jaxb() {
        try {
            jc = JAXBContext.newInstance(StampCoordinateImpl.class,
                    ConceptSpecification.class,
                    ConceptProxy.class, LanguageCoordinateImpl.class,
                    JaxbMap.class, Query.class,
                    Clause.class, Or.class,
                    And.class,
                    AndNot.class,
                    LeafClause.class,
                    Not.class,
                    Or.class,
                    Xor.class,
                    AssemblageContainsConcept.class,
                    AssemblageContainsKindOfConcept.class,
                    AssemblageContainsString.class,
                    AssemblageLuceneMatch.class,
                    ChangedBetweenVersions.class,
                    ComponentIsActive.class,
                    ConceptForComponent.class,
                    ConceptIs.class,
                    ConceptIsChildOf.class,
                    ConceptIsDescendentOf.class,
                    ConceptIsKindOf.class,
                    DescriptionActiveLuceneMatch.class,
                    DescriptionActiveRegexMatch.class,
                    DescriptionLuceneMatch.class,
                    DescriptionRegexMatch.class,
                    FullyQualifiedNameForConcept.class,
                    PreferredNameForConcept.class,
                    RelRestriction.class,
                    RelationshipIsCircular.class,
                    LetItemKey.class,
                    AttributeReturnSpecification.class            
            );
        } catch (JAXBException ex) {
            throw new RuntimeException(ex);
        }
    }
    public static Marshaller createMarshaller() throws JAXBException {
        return jaxb.jc.createMarshaller();
    }
    public static Unmarshaller createUnmarshaller() throws JAXBException {
        return jaxb.jc.createUnmarshaller();
    }
}