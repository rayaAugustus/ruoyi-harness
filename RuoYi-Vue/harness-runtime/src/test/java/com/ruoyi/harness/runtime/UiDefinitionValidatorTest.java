package com.ruoyi.harness.runtime;
import static org.junit.jupiter.api.Assertions.*;import com.ruoyi.harness.api.*;import org.junit.jupiter.api.Test;import tools.jackson.databind.ObjectMapper;
class UiDefinitionValidatorTest {private final ObjectMapper mapper=new ObjectMapper();private final UiDefinitionValidator validator=new UiDefinitionValidator(RuntimeLimits.defaults());
 @Test void rejectsUnknownStructuralFields()throws Exception{HarnessException e=assertThrows(HarnessException.class,()->validator.validatePage(mapper.readTree("{\"type\":\"page\",\"title\":\"x\",\"children\":[],\"onclick\":\"evil()\"}")));assertEquals(HarnessErrorCode.UI_SCHEMA_INVALID,e.getCode());}
 @Test void acceptsHtmlAsTextDataBecauseRendererEscapesIt()throws Exception{assertDoesNotThrow(()->validator.validatePage(mapper.readTree("{\"type\":\"page\",\"title\":\"x\",\"children\":[{\"type\":\"text\",\"value\":\"<script>alert(1)</script>\"}]}")));}}
