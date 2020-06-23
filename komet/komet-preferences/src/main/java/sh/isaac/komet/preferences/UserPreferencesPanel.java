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
package sh.isaac.komet.preferences;

import java.util.ArrayList;
import java.util.List;
import java.util.prefs.BackingStoreException;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.config.Ini;
import org.apache.shiro.config.IniSecurityManagerFactory;
import org.apache.shiro.crypto.hash.Sha256Hash;
import org.apache.shiro.crypto.hash.SimpleHash;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.session.Session;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.Factory;
import sh.isaac.MetaData;
import sh.isaac.api.BusinessRulesResource;
import sh.isaac.api.ConceptProxy;
import sh.isaac.api.Get;
import sh.isaac.api.component.concept.ConceptSpecification;
import sh.isaac.api.preferences.IsaacPreferences;

import static sh.komet.gui.contract.preferences.PreferenceGroup.Keys.GROUP_NAME;

import sh.isaac.model.observable.ObservableFields;
import sh.komet.gui.contract.preferences.KometPreferencesController;
import sh.komet.gui.contract.preferences.UserPreferenceItems;
import sh.komet.gui.control.PropertySheetItemObjectListWrapper;
import sh.komet.gui.control.concept.PropertySheetItemConceptConstraintWrapper;
import sh.komet.gui.control.concept.PropertySheetItemConceptWrapper;
import sh.komet.gui.control.property.SessionProperty;
import sh.komet.gui.control.property.ViewProperties;
import sh.komet.gui.util.FxGet;
import sh.isaac.api.util.UuidStringKey;

/**
 *
 * @author kec
 */
public final class UserPreferencesPanel extends AbstractPreferences implements UserPreferenceItems {
    enum Keys {
        USER_CONCEPT,
        USER_CONCEPT_OPTIONS,
        MODULE_CONCEPT,
        MODULE_CONCEPT_OPTIONS,
        PATH_CONCEPT,
        PATH_CONCEPT_OPTIONS,
        SHIRO_INI,
        DEFAULT_VIEW_KEY
    }
    private static String iniConfig = "Shiro INI";
    private static SecurityManager securityManager = null;


    final SimpleObjectProperty<ConceptSpecification> userConceptProperty = new SimpleObjectProperty<>(this, ObservableFields.KOMET_USER.toExternalString(), MetaData.KOMET_USER____SOLOR);
    final SimpleListProperty<ConceptSpecification> userConceptOptions = new SimpleListProperty(this, ObservableFields.KOMET_USER_LIST.toExternalString(), FXCollections.observableArrayList());
    final PropertySheetItemConceptWrapper userConceptWrapper;

    
    final SimpleObjectProperty<ConceptSpecification> moduleConceptProperty = new SimpleObjectProperty<>(this, ObservableFields.MODULE_FOR_USER.toExternalString(), MetaData.SOLOR_MODULE____SOLOR);
    final SimpleListProperty<ConceptSpecification> moduleConceptOptions = new SimpleListProperty(this, ObservableFields.MODULE_OPTIONS_FOR_EDIT_COORDINATE.toExternalString(), FXCollections.observableArrayList());
    final PropertySheetItemConceptWrapper moduleConceptWrapper;
    
    final SimpleObjectProperty<ConceptSpecification> pathConceptProperty = new SimpleObjectProperty<>(this, ObservableFields.PATH_FOR_USER.toExternalString(), MetaData.DEVELOPMENT_PATH____SOLOR);
    final SimpleListProperty<ConceptSpecification> pathConceptOptions = new SimpleListProperty(this, ObservableFields.PATH_OPTIONS_FOR_EDIT_COORDINATE.toExternalString(), FXCollections.observableArrayList());
    final PropertySheetItemConceptWrapper pathConceptWrapper;
     
    final SimpleStringProperty shiroIniProperty = new SimpleStringProperty(this, "Shiro INI");

    
    public UserPreferencesPanel(IsaacPreferences preferencesNode, ViewProperties viewProperties,
                                KometPreferencesController kpc) {
        super(preferencesNode, preferencesNode.get(GROUP_NAME, "User"), viewProperties,
                kpc);
        this.userConceptWrapper = new PropertySheetItemConceptWrapper(viewProperties, userConceptProperty);
        this.userConceptWrapper.setAllowedValues(userConceptOptions);
        
        this.moduleConceptWrapper = new PropertySheetItemConceptWrapper(viewProperties, moduleConceptProperty);
        this.moduleConceptWrapper.setAllowedValues(moduleConceptOptions);
        
        this.pathConceptWrapper = new PropertySheetItemConceptWrapper(viewProperties, pathConceptProperty);
        this.pathConceptWrapper.setAllowedValues(pathConceptOptions);
        revertFields();
        save();
        int[] userConceptOptionNids = new int[userConceptOptions.size()];
        for (int i = 0; i < userConceptOptionNids.length; i++) {
            userConceptOptionNids[i] = userConceptOptions.get(i).getNid();
        }

        getItemList().add(new PropertySheetItemConceptConstraintWrapper(userConceptWrapper, viewProperties, "User"));
        getItemList().add(new PropertySheetItemConceptConstraintWrapper(moduleConceptWrapper, viewProperties, "Module"));

        login();
    }

    public static void login() {
        if (securityManager == null) {
            Ini ini = new Ini();
            ini.load(makeShiroIni());
            Factory<SecurityManager> factory = new IniSecurityManagerFactory(ini);
            securityManager = factory.getInstance();
            SecurityUtils.setSecurityManager(securityManager);
            Subject currentUser = SecurityUtils.getSubject();
            UsernamePasswordToken token = new UsernamePasswordToken("admin", "mtn.dog");
            currentUser.login(token);
            currentUser.getSession().setAttribute(SessionProperty.EDIT_COORDINATE, FxGet.editCoordinate());

            LOG.info( "User [" + currentUser.getPrincipal() + "] logged in successfully." );

        }
    }

    @Override
    protected void saveFields() throws BackingStoreException {
        preferencesNode.putArray(Keys.DEFAULT_VIEW_KEY, FxGet.defaultViewKey().toStringArray());
        preferencesNode.put(Keys.SHIRO_INI, shiroIniProperty.get());

        preferencesNode.put(Keys.USER_CONCEPT, Get.concept(userConceptProperty.get()).toExternalString());
        List<String> userConceptOptionExternalStrings = new ArrayList<>();
        for (ConceptSpecification spec: userConceptOptions) {
            userConceptOptionExternalStrings.add(spec.toExternalString());
        }
        preferencesNode.putList(Keys.USER_CONCEPT_OPTIONS, userConceptOptionExternalStrings);
        
        preferencesNode.put(Keys.PATH_CONCEPT, Get.concept(pathConceptProperty.get()).toExternalString());
        List<String> pathConceptOptionExternalStrings = new ArrayList<>();
        for (ConceptSpecification spec: pathConceptOptions) {
            pathConceptOptionExternalStrings.add(spec.toExternalString());
        }
        preferencesNode.putList(Keys.PATH_CONCEPT_OPTIONS, pathConceptOptionExternalStrings);
        
        preferencesNode.put(Keys.MODULE_CONCEPT, Get.concept(moduleConceptProperty.get()).toExternalString());
        List<String> moduleConceptOptionExternalStrings = new ArrayList<>();
        for (ConceptSpecification spec: moduleConceptOptions) {
            moduleConceptOptionExternalStrings.add(spec.toExternalString());
        }
        preferencesNode.putList(Keys.MODULE_CONCEPT_OPTIONS, moduleConceptOptionExternalStrings);

        if (securityManager != null) {
            Session session = SecurityUtils.getSubject().getSession();
            session.setAttribute(SessionProperty.USER_SESSION_CONCEPT, Get.conceptSpecification(userConceptProperty.get().getNid()));
        }
        
        // For modules and paths, read/write constraints 
        FxGet.rulesDrivenKometService().addResourcesAndUpdate(getBusinessRulesResources());
        FxGet.editCoordinate().authorProperty().set(userConceptProperty.get());
        FxGet.editCoordinate().moduleProperty().set(moduleConceptProperty.get());
        FxGet.editCoordinate().pathProperty().set(pathConceptProperty.get());
    }

    @Override
    protected void revertFields() {

        if (preferencesNode.hasKey(Keys.DEFAULT_VIEW_KEY)) {
            UuidStringKey viewKey = new UuidStringKey(preferencesNode.getArray(Keys.DEFAULT_VIEW_KEY));
            FxGet.setDefaultViewKey(viewKey);
        }

        shiroIniProperty.set(preferencesNode.get(Keys.SHIRO_INI, makeShiroIni()));

        List<String> userConceptOptionExternalStrings = preferencesNode.getList(Keys.USER_CONCEPT_OPTIONS);
        if (userConceptOptionExternalStrings.isEmpty()) {
            userConceptOptionExternalStrings.add(MetaData.USER____SOLOR.toExternalString());
            userConceptOptionExternalStrings.add(MetaData.BOOTSTRAP_ADMINISTRATOR____SOLOR.toExternalString());
            userConceptOptionExternalStrings.add(MetaData.KEITH_EUGENE_CAMPBELL____SOLOR.toExternalString());
            userConceptOptionExternalStrings.add(MetaData.DELOITTE_USER____SOLOR.toExternalString());
        }
        userConceptOptions.clear();
        for (String externalString: userConceptOptionExternalStrings) {
            userConceptOptions.add(new ConceptProxy(externalString));
        }
        String userConceptSpec = preferencesNode.get(Keys.USER_CONCEPT, MetaData.USER____SOLOR.toExternalString());
        userConceptProperty.set(new ConceptProxy(userConceptSpec));
        
        
        List<String> pathConceptOptionExternalStrings = preferencesNode.getList(Keys.PATH_CONCEPT_OPTIONS);
        if (pathConceptOptionExternalStrings.isEmpty()) {
            pathConceptOptionExternalStrings.add(MetaData.MASTER_PATH____SOLOR.toExternalString());
            pathConceptOptionExternalStrings.add(MetaData.DEVELOPMENT_PATH____SOLOR.toExternalString());
        }
        pathConceptOptions.clear();
        for (String externalString: pathConceptOptionExternalStrings) {
            pathConceptOptions.add(new ConceptProxy(externalString));
        }
        String pathConceptSpec = preferencesNode.get(Keys.PATH_CONCEPT, MetaData.DEVELOPMENT_PATH____SOLOR.toExternalString());
        pathConceptProperty.set(new ConceptProxy(pathConceptSpec));
        
        List<String> moduleConceptOptionExternalStrings = preferencesNode.getList(Keys.MODULE_CONCEPT_OPTIONS);
        if (moduleConceptOptionExternalStrings.isEmpty()) {
            moduleConceptOptionExternalStrings.add(MetaData.SOLOR_MODULE____SOLOR.toExternalString());
            moduleConceptOptionExternalStrings.add(MetaData.SOLOR_OVERLAY_MODULE____SOLOR.toExternalString());
        }
        moduleConceptOptions.clear();
        for (String externalString: moduleConceptOptionExternalStrings) {
            moduleConceptOptions.add(new ConceptProxy(externalString));
        }
        String moduleConceptSpec = preferencesNode.get(Keys.MODULE_CONCEPT, MetaData.SOLOR_MODULE____SOLOR.toExternalString());
        moduleConceptProperty.set(new ConceptProxy(moduleConceptSpec));

    }
    
    static String makeShiroIni() {
        
        StringBuilder b = new StringBuilder();
        b.append("[main]\n");
        b.append("sha256Matcher = org.apache.shiro.authc.credential.Sha256CredentialsMatcher\n");
        b.append("sha256Matcher.storedCredentialsHexEncoded = false\n");
        b.append("iniRealm.credentialsMatcher = $sha256Matcher\n");
        b.append("securityManager.sessionManager.globalSessionTimeout = -1\n");
        b.append("\n[users]\n");
        UsernamePasswordToken token = new UsernamePasswordToken("admin", "mtn.dog");
        SimpleHash hash = new SimpleHash(Sha256Hash.ALGORITHM_NAME, token.getCredentials());
        
        b.append("admin = ").append(hash.toBase64()).append(", admin\n");
        b.append("\n[roles]\n");
        b.append("admin = *\n");
        
        
        return b.toString();
    }
    public BusinessRulesResource[] getBusinessRulesResources() {
        List<BusinessRulesResource> resources = new ArrayList<>();

        resources.add(new BusinessRulesResource(
                "sh/isaac/provider/drools/" + preferencesNode.name() + ".drl",
                getRuleBytes()));

        return resources.toArray(new BusinessRulesResource[resources.size()]);
    }

    private byte[] getRuleBytes() {
        StringBuilder b = new StringBuilder();
        b.append("package sh.isaac.provider.drools;\n");
        b.append("import java.util.ArrayList;\n");
        b.append("import java.util.List;\n");
        b.append("import java.util.Map;\n");
        b.append("import java.util.UUID;\n");
        b.append("import java.util.function.Consumer;\n");
        b.append("import javafx.scene.control.MenuItem;\n");
        b.append("import javafx.beans.property.Property;\n");
        b.append("import org.controlsfx.control.PropertySheet;\n");
        b.append("import org.controlsfx.control.PropertySheet.Item;\n");
        b.append("import sh.isaac.api.observable.ObservableCategorizedVersion;\n");
        b.append("import sh.isaac.api.ConceptProxy;\n");
        b.append("import sh.isaac.api.Get;\n");
        b.append("import sh.isaac.api.component.concept.ConceptSpecification;\n");
        b.append("import sh.isaac.api.Status;\n");
        b.append("import sh.isaac.provider.drools.AddEditLogicalExpressionNodeMenuItems;\n");
        b.append("import sh.komet.gui.control.PropertySheetMenuItem;\n");
        b.append("import sh.komet.gui.manifold.Manifold;\n");
        b.append("import sh.isaac.MetaData;\n");
        b.append("import sh.isaac.api.bootstrap.TermAux;\n");
        b.append("import sh.isaac.api.chronicle.VersionCategory;\n");
        b.append("import sh.isaac.api.chronicle.VersionType;\n");
        b.append("import sh.isaac.provider.drools.AddAttachmentMenuItems;\n");
        b.append("import sh.komet.gui.control.PropertyEditorType;\n");
        b.append("import sh.komet.gui.control.concept.PropertySheetItemConceptNidWrapper;\n");
        b.append("import sh.komet.gui.control.concept.PropertySheetItemConceptWrapper;\n");
        b.append("import sh.komet.gui.control.property.PropertySheetItem;\n");
        b.append("import sh.komet.gui.control.property.PropertySheetPurpose;\n");
        b.append("import sh.komet.gui.control.property.EditorType;\n");
        b.append("import sh.isaac.api.logic.NodeSemantic;\n");
        b.append("\n");
        b.append("rule \"if property specification is PATH_NID_FOR_VERSION____SOLOR ").append(preferencesNode.name()).append("\"\n");
        b.append("when\n");
        b.append("   $property: PropertySheetItemConceptWrapper(getSpecification() == MetaData.PATH_NID_FOR_VERSION____SOLOR)\n");
        b.append("then\n");
        b.append("   $property.setDefaultValue(new ").append(new ConceptProxy(pathConceptProperty.get()).toString()).append(");\n");
        
        for (ConceptSpecification pathSpec: pathConceptOptions) {
            b.append("   $property.getAllowedValues().add(new ").append(new ConceptProxy(pathSpec).toString()).append(");\n");
        }
        b.append("end\n\n");

        b.append("rule \"if property specification is MODULE_NID_FOR_VERSION____SOLOR ").append(preferencesNode.name()).append("\"\n");
        b.append("when\n");
        b.append("   $property: PropertySheetItemConceptWrapper(getSpecification() == MetaData.MODULE_NID_FOR_VERSION____SOLOR)\n");
        b.append("then\n");
        b.append("   $property.setDefaultValue(new ").append(new ConceptProxy(moduleConceptProperty.get()).toString()).append(");\n");
        
        for (ConceptSpecification moduleSpec: moduleConceptOptions) {
            b.append("   $property.getAllowedValues().add(new ").append(new ConceptProxy(moduleSpec).toString())
                .append(");\n");
        }
        b.append("end\n\n");
//
//
//

        b.append("rule \"DEPRECATED if property specification is PATH_NID_FOR_VERSION____SOLOR ").append(preferencesNode.name()).append("\"\n");
        b.append("when\n");
        b.append("   $property: PropertySheetItem(getSpecification() == MetaData.PATH_NID_FOR_VERSION____SOLOR)\n");
        b.append("then\n");
        b.append("   $property.setDefaultValue(new ").append(new ConceptProxy(pathConceptProperty.get()).toString()).append(");\n");
        
        for (ConceptSpecification pathSpec: pathConceptOptions) {
            b.append("   $property.getAllowedValues().add(new ").append(new ConceptProxy(pathSpec).toString()).append(");\n");
        }
        b.append("end\n\n");

        b.append("rule \"DEPRECATED if property specification is MODULE_NID_FOR_VERSION____SOLOR ").append(preferencesNode.name()).append("\"\n");
        b.append("when\n");
        b.append("   $property: PropertySheetItem(getSpecification() == MetaData.MODULE_NID_FOR_VERSION____SOLOR)\n");
        b.append("then\n");
        b.append("   $property.setDefaultValue(new ").append(new ConceptProxy(moduleConceptProperty.get()).toString()).append(");\n");
        
        for (ConceptSpecification moduleSpec: moduleConceptOptions) {
            b.append("   $property.getAllowedValues().add(new ").append(new ConceptProxy(moduleSpec).toString())
                .append(");\n");
        }
        b.append("end\n\n");

        return b.toString().getBytes();
    }
}
    

