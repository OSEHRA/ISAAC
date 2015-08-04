package gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe;

import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.dataTypes.DynamicSememeArrayBI;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.dataTypes.DynamicSememeBooleanBI;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.dataTypes.DynamicSememeByteArrayBI;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.dataTypes.DynamicSememeDoubleBI;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.dataTypes.DynamicSememeFloatBI;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.dataTypes.DynamicSememeIntegerBI;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.dataTypes.DynamicSememeLongBI;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.dataTypes.DynamicSememeNidBI;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.dataTypes.DynamicSememePolymorphicBI;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.dataTypes.DynamicSememeStringBI;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.dataTypes.DynamicSememeUUIDBI;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.UUID;


/**
 * 
 * {@link DynamicSememeDataType}
 * 
 * Most types are fairly straight forward.  NIDs and INTEGERS are identical internally, except NIDs identify concepts.
 * Polymorphic is used when the data type for a refex isn't known at refex creation time.  In this case, a user of the API
 * will have to examine type types of the actual {@link DynamicSememeDataBI} objects returned, to look at the type.
 * 
 * For all other types, the data type reported within the Refex Definition should exactly match the data type returned with 
 * a {@link DynamicSememeDataBI}.
 * 
 * {@link DynamicSememeDataBI} will never return a {@link POLYMORPHIC} type.
 *
 * @author kec
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a>
 */
public enum DynamicSememeDataType {
	
	NID(101, DynamicSememeNidBI.class, "Component Nid"),
	STRING(102, DynamicSememeStringBI.class, "String"),
	INTEGER(103, DynamicSememeIntegerBI.class, "Integer"),
	BOOLEAN(104, DynamicSememeBooleanBI.class, "Boolean"),
	LONG(105, DynamicSememeLongBI.class, "Long"),
	BYTEARRAY(106, DynamicSememeByteArrayBI.class, "Arbitrary Data"),
	FLOAT(107, DynamicSememeFloatBI.class, "Float"),
	DOUBLE(108, DynamicSememeDoubleBI.class, "Double"),
	UUID(109, DynamicSememeUUIDBI.class, "UUID"),
	POLYMORPHIC(110, DynamicSememePolymorphicBI.class, "Unspecified"),
	ARRAY(111, DynamicSememeArrayBI.class, "Array"),
	UNKNOWN(Byte.MAX_VALUE, null, "Unknown");

	private int externalizedToken_;
	private Class<? extends DynamicSememeDataBI> dataClass_;
	private String displayName_;

	public static DynamicSememeDataType getFromToken(int type) throws UnsupportedOperationException {
		switch (type) {
			case 101:
				return NID;
			case 102:
				return STRING;
			case 103:
				return INTEGER;
			case 104:
				return BOOLEAN;
			case 105:
				return LONG;
			case 106:
				return BYTEARRAY;
			case 107:
				return FLOAT;
			case 108:
				return DOUBLE;
			case 109:
				return UUID;
			case 110:
				return POLYMORPHIC;
			case 111:
				return ARRAY;
			default:
				return UNKNOWN;
		}
	}
	
	private DynamicSememeDataType(int externalizedToken, Class<? extends DynamicSememeDataBI> dataClass, String displayName)
	{
		externalizedToken_ = externalizedToken;
		dataClass_ = dataClass;
		displayName_ = displayName;
	}

	public int getTypeToken()
	{
		return this.externalizedToken_;
	}

	public Class<? extends DynamicSememeDataBI> getRefexMemberClass()
	{
		return dataClass_;
	}
	
	public UUID getDataTypeConcept()
	{
		/*
		 * Implementation note - these used to be defined in the constructor, and stored in a local variable - but
		 * that lead to a circular loop between the references of static elements in this class and DynamicSememe, 
		 * specifically in the constructors - which would throw maven / surefire for a loop - resulting in a 
		 * class not found exception... which was a PITA to track down.  So, don't do that....
		 */
		switch (this)
		{
			case BOOLEAN: return DynamicSememeConstants.DYNAMIC_SEMEME_DT_BOOLEAN.getUuids()[0];
			case BYTEARRAY: return DynamicSememeConstants.DYNAMIC_SEMEME_DT_BYTE_ARRAY.getUuids()[0];
			case DOUBLE: return DynamicSememeConstants.DYNAMIC_SEMEME_DT_DOUBLE.getUuids()[0];
			case FLOAT: return DynamicSememeConstants.DYNAMIC_SEMEME_DT_FLOAT.getUuids()[0];
			case INTEGER: return DynamicSememeConstants.DYNAMIC_SEMEME_DT_INTEGER.getUuids()[0];
			case LONG: return DynamicSememeConstants.DYNAMIC_SEMEME_DT_LONG.getUuids()[0];
			case NID: return DynamicSememeConstants.DYNAMIC_SEMEME_DT_NID.getUuids()[0];
			case POLYMORPHIC: return DynamicSememeConstants.DYNAMIC_SEMEME_DT_POLYMORPHIC.getUuids()[0];
			case STRING: return DynamicSememeConstants.DYNAMIC_SEMEME_DT_STRING.getUuids()[0];
			case UNKNOWN: return DynamicSememeConstants.UNKNOWN_CONCEPT.getUuids()[0];
			case UUID: return DynamicSememeConstants.DYNAMIC_SEMEME_DT_UUID.getUuids()[0];
			case ARRAY: return DynamicSememeConstants.DYNAMIC_SEMEME_DT_ARRAY.getUuids()[0];

			default: throw new RuntimeException("Implementation error");
		}
	}
	
	public String getDisplayName()
	{
		return displayName_;
	}

	public void writeType(DataOutput output) throws IOException
	{
		output.writeByte(externalizedToken_);
	}

	public static DynamicSememeDataType classToType(Class<?> c) 
	{
		if (DynamicSememeNidBI.class.isAssignableFrom(c)) {
			return NID;
		}
		if (DynamicSememeStringBI.class.isAssignableFrom(c)) {
			return STRING;
		}
		if (DynamicSememeIntegerBI.class.isAssignableFrom(c)) {
			return INTEGER;
		}
		if (DynamicSememeBooleanBI.class.isAssignableFrom(c)) {
			return BOOLEAN;
		}
		if (DynamicSememeLongBI.class.isAssignableFrom(c)) {
			return LONG;
		}
		if (DynamicSememeByteArrayBI.class.isAssignableFrom(c)) {
			return BYTEARRAY;
		}
		if (DynamicSememeFloatBI.class.isAssignableFrom(c)) {
			return FLOAT;
		}
		if (DynamicSememeDoubleBI.class.isAssignableFrom(c)) {
			return DOUBLE;
		}
		if (DynamicSememeUUIDBI.class.isAssignableFrom(c)) {
			return UUID;
		}
		if (DynamicSememePolymorphicBI.class.isAssignableFrom(c)) {
			return POLYMORPHIC;
		}
		if (DynamicSememeArrayBI.class.isAssignableFrom(c)) {
			return ARRAY;
		}
		return UNKNOWN;
	}

	public static DynamicSememeDataType readType(DataInput input) throws IOException
	{
		int type = input.readByte();
		return getFromToken(type);
	}
}