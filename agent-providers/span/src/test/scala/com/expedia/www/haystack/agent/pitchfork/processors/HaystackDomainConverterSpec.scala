package com.expedia.www.haystack.agent.pitchfork.processors

import com.expedia.open.tracing.Tag
import org.scalatest.{FunSpec, Matchers}
import org.scalatest.easymock.EasyMockSugar
import zipkin2.{Endpoint, Span}

class HaystackDomainConverterSpec extends FunSpec with Matchers with EasyMockSugar {

  private def zipkinSpanBuilder(traceId: String): Span.Builder = {
    zipkin2.Span.newBuilder()
      .traceId(traceId)
      .id(1)
      .parentId(2)
      .name("/foo")
      .localEndpoint(Endpoint.newBuilder().serviceName("foo").build())
      .remoteEndpoint(Endpoint.newBuilder().serviceName("bar").port(8080).ip("10.10.10.10").build())
      .timestamp(System.currentTimeMillis() * 1000)
      .duration(100000l)
      .putTag("pos", "1")
  }

  describe("Haystack Domain Converter") {
    it("should create span from Zipkin span") {
      val traceId = "bd1068b1bc333ec0"
      val zipkinSpan = zipkinSpanBuilder(traceId).clearTags().build()
      val span = HaystackDomainConverter.fromZipkinV2(zipkinSpan)

      span.getTraceId shouldBe traceId
      span.getTagsList.stream().filter(_.getKey == "error").count() shouldBe 0
    }

    it("should create span from Zipkin span with error false") {
      val traceId = "bd1068b1bc333ec0"
      val zipkinSpan = zipkinSpanBuilder(traceId).putTag("error", "false").build()
      val span = HaystackDomainConverter.fromZipkinV2(zipkinSpan)

      span.getTraceId shouldBe traceId
      span.getTagsList.stream().filter(_.getKey == "error").count() shouldBe 1
      span.getTagsList.stream().filter(tag => tag.getKey == "error" && tag.getType == Tag.TagType.BOOL && !tag.getVBool).count() shouldBe 1
    }

    it("should create span from Zipkin span with error true") {
      val traceId = "bd1068b1bc333ec0"
      val zipkinSpan = zipkinSpanBuilder(traceId).putTag("error", "bad things").build()
      val span = HaystackDomainConverter.fromZipkinV2(zipkinSpan)

      span.getTraceId shouldBe traceId
      span.getTagsList.stream().filter(_.getKey == "error").count() shouldBe 1
      span.getTagsList.stream().filter(tag => tag.getKey == "error" && tag.getType == Tag.TagType.BOOL && tag.getVBool).count() shouldBe 1
      span.getTagsList.stream().filter(_.getKey == "error_msg").count() shouldBe 1
    }

    it("should create span with kind tag") {
      val traceId = "edcb04102634b702"
      val zipkinSpan = zipkinSpanBuilder(traceId)
        .kind(Span.Kind.SERVER)
        .clearTags()
        .build()
      val span = HaystackDomainConverter.fromZipkinV2(zipkinSpan)

      span.getTraceId shouldBe traceId
      span.getTagsList.stream().filter(_.getKey == "span.kind").count() shouldBe 1
    }

    it("should create span without duplicate kind tag") {
      val traceId = "661e251d4406e110"
      val zipkinSpan = zipkinSpanBuilder(traceId).kind(Span.Kind.SERVER).putTag("span.kind", "server").build()
      val span = HaystackDomainConverter.fromZipkinV2(zipkinSpan)

      span.getTraceId shouldBe traceId
      span.getTagsList.stream().filter(_.getKey == "span.kind").count() shouldBe 1
    }
  }

}
