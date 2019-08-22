// Copyright 2017 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.api.graphql.rejoiner;

import static com.google.common.truth.Truth.assertThat;

import com.google.api.graphql.rejoiner.TestProto.Proto1;
import com.google.api.graphql.rejoiner.TestProto.Proto1.InnerProto;
import com.google.api.graphql.rejoiner.TestProto.Proto2;
import com.google.api.graphql.rejoiner.TestProto.Proto2.TestEnum;
import com.google.common.collect.ImmutableMap;
import graphql.schema.GraphQLEnumType;
import graphql.schema.GraphQLEnumValueDefinition;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLObjectType;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Unit tests for {@link com.google.api.graphql.rejoiner.ProtoToGql}. */
@RunWith(JUnit4.class)
public final class ProtoToGqlTest {

  @Test
  public void getReferenceNameShouldReturnCorrectValueForMessages() {
    assertThat(ProtoToGql.getReferenceName(Proto1.getDescriptor()))
        .isEqualTo("javatests_com_google_api_graphql_rejoiner_proto_Proto1");
    assertThat(ProtoToGql.getReferenceName(Proto2.getDescriptor()))
        .isEqualTo("javatests_com_google_api_graphql_rejoiner_proto_Proto2");
  }

  @Test
  public void getReferenceNameShouldReturnCorrectValueForInnerMessages() {
    assertThat(ProtoToGql.getReferenceName(InnerProto.getDescriptor()))
        .isEqualTo("javatests_com_google_api_graphql_rejoiner_proto_Proto1_InnerProto");
  }

  @Test
  public void getReferenceNameShouldReturnCorrectValueForEnums() {
    assertThat(ProtoToGql.getReferenceName(TestEnum.getDescriptor()))
        .isEqualTo("javatests_com_google_api_graphql_rejoiner_proto_Proto2_TestEnum");
  }

  @Test
  public void convertShouldWorkForMessage() {
    GraphQLObjectType result = ProtoToGql.convert(Proto1.getDescriptor(), null, ImmutableMap.of());
    assertThat(result.getName())
        .isEqualTo("javatests_com_google_api_graphql_rejoiner_proto_Proto1");
    assertThat(result.getFieldDefinitions()).hasSize(7);
  }

  @Test
  public void convertShouldWorkForEnums() {
    GraphQLEnumType result = ProtoToGql.convert(TestEnum.getDescriptor(), ImmutableMap.of());
    assertThat(result.getName())
        .isEqualTo("javatests_com_google_api_graphql_rejoiner_proto_Proto2_TestEnum");
    assertThat(result.getValues()).hasSize(3);
    assertThat(result.getValues().stream().map(GraphQLEnumValueDefinition::getName).toArray())
        .asList()
        .containsExactly("UNKNOWN", "FOO", "BAR");
  }

  @Test
  public void checkFieldNameCamelCase() {
    GraphQLObjectType result = ProtoToGql.convert(Proto1.getDescriptor(), null, ImmutableMap.of());
    assertThat(result.getFieldDefinitions()).hasSize(7);
    assertThat(result.getFieldDefinition("intField")).isNotNull();
    assertThat(result.getFieldDefinition("camelCaseName")).isNotNull();
    assertThat(result.getFieldDefinition("RenamedField")).isNotNull();
  }

  @Test
  public void checkComments() {

    String DEFAULT_DESCRIPTOR_SET_FILE_LOCATION = "META-INF/proto/descriptor_set.desc";

    ImmutableMap<String, String> comments =
        DescriptorSet.getCommentsFromDescriptorFile(
            DescriptorSet.class
                .getClassLoader()
                .getResourceAsStream(DEFAULT_DESCRIPTOR_SET_FILE_LOCATION));

    GraphQLObjectType result = ProtoToGql.convert(Proto1.getDescriptor(), null, comments);

    GraphQLFieldDefinition intFieldGraphQLFieldDefinition = result.getFieldDefinition("intField");
    assertThat(intFieldGraphQLFieldDefinition).isNotNull();
    assertThat(
            intFieldGraphQLFieldDefinition
                .getDescription()
                .equals("Some leading comment. Some trailing comment"))
        .isTrue();

    GraphQLFieldDefinition camelCaseNameGraphQLFieldDefinition =
        result.getFieldDefinition("camelCaseName");
    assertThat(camelCaseNameGraphQLFieldDefinition).isNotNull();
    assertThat(camelCaseNameGraphQLFieldDefinition.getDescription().equals("Some leading comment"))
        .isTrue();

    GraphQLFieldDefinition testProtoNameGraphQLFieldDefinition =
        result.getFieldDefinition("testProto");
    assertThat(testProtoNameGraphQLFieldDefinition).isNotNull();
    assertThat(testProtoNameGraphQLFieldDefinition.getDescription().equals("Some trailing comment"))
        .isTrue();

    GraphQLEnumType nestedEnumType = ProtoToGql.convert(TestEnum.getDescriptor(), comments);

    GraphQLEnumValueDefinition graphQLFieldDefinition = nestedEnumType.getValue("UNKNOWN");
    assertThat(graphQLFieldDefinition.getDescription().equals("Some trailing comment")).isTrue();

    GraphQLObjectType nestedMessageType =
        ProtoToGql.convert(Proto2.NestedProto.getDescriptor(), null, comments);

    assertThat(nestedMessageType.getDescription().equals("Nested type comment")).isTrue();

    GraphQLFieldDefinition nestedFieldGraphQLFieldDefinition =
        nestedMessageType.getFieldDefinition("nestedId");
    assertThat(nestedFieldGraphQLFieldDefinition).isNotNull();
    assertThat(nestedFieldGraphQLFieldDefinition.getDescription().equals("Some nested id"))
        .isTrue();

    GraphQLEnumType enumType =
        ProtoToGql.convert(TestProto.TestEnumWithComments.getDescriptor(), comments);

    graphQLFieldDefinition = enumType.getValue("FOO");
    assertThat(graphQLFieldDefinition.getDescription().equals("Some trailing comment")).isTrue();
  }
}
