//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.3.1-b171012.0423 
//         See <a href="https://javaee.github.io/jaxb-v2/">https://javaee.github.io/jaxb-v2/</a> 
//         Any modifications to this file will be lost upon recompilation of the source schema. 
//         Generated on: 2018.07.22 at 12:40:28 AM CDT 
//


package ca.wise.lib.xml;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for statusType.
 * 
 * <p>The following schema fragment specifies the expected         content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="statusType"&gt;
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string"&gt;
 *     &lt;enumeration value="Submitted"/&gt;
 *     &lt;enumeration value="Started"/&gt;
 *     &lt;enumeration value="ScenarioStarted"/&gt;
 *     &lt;enumeration value="ScenarioCompleted"/&gt;
 *     &lt;enumeration value="ScenarioFailed"/&gt;
 *     &lt;enumeration value="Complete"/&gt;
 *     &lt;enumeration value="Failed"/&gt;
 *     &lt;enumeration value="Error"/&gt;
 *     &lt;enumeration value="Information"/&gt;
 *   &lt;/restriction&gt;
 * &lt;/simpleType&gt;
 * </pre>
 * 
 */
@XmlType(name = "statusType")
@XmlEnum
public enum StatusType {

    @XmlEnumValue("Submitted")
    SUBMITTED("Submitted"),
    @XmlEnumValue("Started")
    STARTED("Started"),
    @XmlEnumValue("ScenarioStarted")
    SCENARIO_STARTED("ScenarioStarted"),
    @XmlEnumValue("ScenarioCompleted")
    SCENARIO_COMPLETED("ScenarioCompleted"),
    @XmlEnumValue("ScenarioFailed")
    SCENARIO_FAILED("ScenarioFailed"),
    @XmlEnumValue("Complete")
    COMPLETE("Complete"),
    @XmlEnumValue("Failed")
    FAILED("Failed"),
    @XmlEnumValue("Error")
    ERROR("Error"),
    @XmlEnumValue("Information")
    INFORMATION("Information");
    private final String value;

    StatusType(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static StatusType fromValue(String v) {
        for (StatusType c: StatusType.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}