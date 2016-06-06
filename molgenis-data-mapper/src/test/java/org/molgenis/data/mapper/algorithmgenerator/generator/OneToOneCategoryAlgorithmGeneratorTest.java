package org.molgenis.data.mapper.algorithmgenerator.generator;

import static org.molgenis.MolgenisFieldTypes.FieldTypeEnum.CATEGORICAL;
import static org.molgenis.MolgenisFieldTypes.FieldTypeEnum.INT;
import static org.molgenis.MolgenisFieldTypes.FieldTypeEnum.STRING;
import static org.molgenis.data.meta.EntityMetaData.AttributeRole.ROLE_ID;
import static org.molgenis.data.meta.EntityMetaData.AttributeRole.ROLE_LABEL;

import java.util.Arrays;
import java.util.stream.Stream;

import org.mockito.Mockito;
import org.molgenis.data.DataService;
import org.molgenis.data.meta.AttributeMetaData;
import org.molgenis.data.meta.EntityMetaData;
import org.molgenis.data.support.MapEntity;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

public class OneToOneCategoryAlgorithmGeneratorTest
{
	AbstractCategoryAlgorithmGenerator categoryAlgorithmGenerator;

	AttributeMetaData targetAttributeMetaData;

	AttributeMetaData sourceAttributeMetaData;

	EntityMetaData targetEntityMetaData;

	EntityMetaData sourceEntityMetaData;

	DataService dataService;

	@BeforeMethod
	public void init()
	{
		dataService = Mockito.mock(DataService.class);

		categoryAlgorithmGenerator = new OneToOneCategoryAlgorithmGenerator(dataService);

		EntityMetaData targetRefEntityMetaData = createCategoricalRefEntityMetaData("POTATO_REF");
		MapEntity targetEntity1 = new MapEntity(ImmutableMap.of("code", 1, "label", "Almost daily + daily"));
		MapEntity targetEntity2 = new MapEntity(ImmutableMap.of("code", 2, "label", "Several times a week"));
		MapEntity targetEntity3 = new MapEntity(ImmutableMap.of("code", 3, "label", "About once a week"));
		MapEntity targetEntity4 = new MapEntity(ImmutableMap.of("code", 4, "label", "Never + fewer than once a week"));
		MapEntity targetEntity5 = new MapEntity(ImmutableMap.of("code", 9, "label", "missing"));

		targetAttributeMetaData = new AttributeMetaData("Current Consumption Frequency of Potatoes",
				CATEGORICAL);
		targetAttributeMetaData.setRefEntity(targetRefEntityMetaData);

		Mockito.when(dataService.findAll(targetRefEntityMetaData.getName()))
				.thenReturn(Stream.of(targetEntity1, targetEntity2, targetEntity3, targetEntity4, targetEntity5));

		targetEntityMetaData = new EntityMetaData("target");
		targetEntityMetaData.addAttribute(targetAttributeMetaData);

		EntityMetaData sourceRefEntityMetaData = createCategoricalRefEntityMetaData("LifeLines_POTATO_REF");
		MapEntity sourceEntity1 = new MapEntity(ImmutableMap.of("code", 1, "label", "Not this month"));
		MapEntity sourceEntity2 = new MapEntity(ImmutableMap.of("code", 2, "label", "1 day per month"));
		MapEntity sourceEntity3 = new MapEntity(ImmutableMap.of("code", 3, "label", "2-3 days per month"));
		MapEntity sourceEntity4 = new MapEntity(ImmutableMap.of("code", 4, "label", "1 day per week"));
		MapEntity sourceEntity5 = new MapEntity(ImmutableMap.of("code", 5, "label", "2-3 days per week"));
		MapEntity sourceEntity6 = new MapEntity(ImmutableMap.of("code", 6, "label", "4-5 days per week"));
		MapEntity sourceEntity7 = new MapEntity(ImmutableMap.of("code", 7, "label", "6-7 days per week"));
		MapEntity sourceEntity8 = new MapEntity(ImmutableMap.of("code", 8, "label", "9 days per week"));

		sourceAttributeMetaData = new AttributeMetaData("MESHED_POTATO", CATEGORICAL);
		sourceAttributeMetaData.setLabel(
				"How often did you eat boiled or mashed potatoes (also in stew) in the past month? Baked potatoes are asked later");
		sourceAttributeMetaData.setRefEntity(sourceRefEntityMetaData);

		Mockito.when(dataService.findAll(sourceRefEntityMetaData.getName()))
				.thenReturn(Stream.of(sourceEntity1, sourceEntity2, sourceEntity3, sourceEntity4, sourceEntity5,
						sourceEntity6, sourceEntity7, sourceEntity8));

		sourceEntityMetaData = new EntityMetaData("source");
		sourceEntityMetaData.addAttributes(Lists.newArrayList(sourceAttributeMetaData));
	}

	@Test
	public void testIsSuitable()
	{
		Assert.assertTrue(
				categoryAlgorithmGenerator.isSuitable(targetAttributeMetaData, Arrays.asList(sourceAttributeMetaData)));
	}

	@Test
	public void testGenerate()
	{
		String generatedAlgorithm = categoryAlgorithmGenerator.generate(targetAttributeMetaData,
				Arrays.asList(sourceAttributeMetaData), targetEntityMetaData, sourceEntityMetaData);
		String expectedAlgorithm = "$('MESHED_POTATO').map({\"1\":\"4\",\"2\":\"4\",\"3\":\"4\",\"4\":\"3\",\"5\":\"2\",\"6\":\"2\",\"7\":\"1\",\"8\":\"1\"}, null, null).value();";

		Assert.assertEquals(generatedAlgorithm, expectedAlgorithm);
	}

	@Test
	public void testGenerateRules()
	{
		EntityMetaData targetRefEntityMetaData = createCategoricalRefEntityMetaData("HOP_HYPERTENSION");
		MapEntity targetEntity1 = new MapEntity(ImmutableMap.of("code", 0, "label", "Never had high blood pressure "));
		MapEntity targetEntity2 = new MapEntity(ImmutableMap.of("code", 1, "label", "Ever had high blood pressure "));
		MapEntity targetEntity3 = new MapEntity(ImmutableMap.of("code", 9, "label", "Missing"));
		Mockito.when(dataService.findAll(targetRefEntityMetaData.getName()))
				.thenReturn(Stream.of(targetEntity1, targetEntity2, targetEntity3));
		targetAttributeMetaData = new AttributeMetaData("History of Hypertension", CATEGORICAL);
		targetAttributeMetaData.setRefEntity(targetRefEntityMetaData);

		EntityMetaData sourceRefEntityMetaData = createCategoricalRefEntityMetaData("High_blood_pressure_ref");
		MapEntity sourceEntity1 = new MapEntity(ImmutableMap.of("code", 1, "label", "yes"));
		MapEntity sourceEntity2 = new MapEntity(ImmutableMap.of("code", 2, "label", "no"));
		MapEntity sourceEntity3 = new MapEntity(ImmutableMap.of("code", 3, "label", "I do not know"));
		Mockito.when(dataService.findAll(sourceRefEntityMetaData.getName()))
				.thenReturn(Stream.of(sourceEntity1, sourceEntity2, sourceEntity3));

		sourceAttributeMetaData = new AttributeMetaData("High_blood_pressure", CATEGORICAL);
		sourceAttributeMetaData.setRefEntity(sourceRefEntityMetaData);

		String generatedAlgorithm = categoryAlgorithmGenerator.generate(targetAttributeMetaData,
				Arrays.asList(sourceAttributeMetaData), targetEntityMetaData, sourceEntityMetaData);

		String expectedAlgorithm = "$('High_blood_pressure').map({\"1\":\"1\",\"2\":\"0\",\"3\":\"9\"}, null, null).value();";

		Assert.assertEquals(generatedAlgorithm, expectedAlgorithm);
	}

	private EntityMetaData createCategoricalRefEntityMetaData(String entityName)
	{
		EntityMetaData targetRefEntityMetaData = new EntityMetaData(entityName);
		AttributeMetaData targetCodeAttributeMetaData = new AttributeMetaData("code", INT);
		AttributeMetaData targetLabelAttributeMetaData = new AttributeMetaData("label", STRING);
		targetRefEntityMetaData.addAttribute(targetCodeAttributeMetaData, ROLE_ID);
		targetRefEntityMetaData.addAttribute(targetLabelAttributeMetaData, ROLE_LABEL);
		return targetRefEntityMetaData;
	}
}
