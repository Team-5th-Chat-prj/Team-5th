package com.clone.getchu.domain.chat.controller;

import com.clone.getchu.domain.chat.dto.request.CreateChatRoomRequest;
import com.clone.getchu.domain.chat.dto.response.ChatMessageResponse;
import com.clone.getchu.domain.chat.dto.response.ChatRoomResponse;
import com.clone.getchu.domain.chat.dto.response.ChatRoomSummaryResponse;
import com.clone.getchu.domain.chat.service.ChatMessageService;
import com.clone.getchu.domain.chat.service.ChatRoomService;
import com.clone.getchu.global.common.CursorPageResponse;
import com.clone.getchu.support.RestDocsSupport;
import com.clone.getchu.support.WithMockCustomUser;
import com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper;
import com.epages.restdocs.apispec.ResourceSnippetParameters;
import com.epages.restdocs.apispec.Schema;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;

import java.time.LocalDateTime;
import java.util.List;

import static com.epages.restdocs.apispec.ResourceDocumentation.headerWithName;
import static com.epages.restdocs.apispec.ResourceDocumentation.parameterWithName;
import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ChatRoomController.class)
class ChatRoomControllerTest extends RestDocsSupport {

    @MockBean
    private ChatRoomService chatRoomService;

    @MockBean
    private ChatMessageService chatMessageService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("채팅방 생성 - POST /chat-rooms")
    @WithMockCustomUser
    void createOrGetChatRoom_created() throws Exception {
        // given
        CreateChatRoomRequest request = new CreateChatRoomRequest(1L);
        ChatRoomResponse response = new ChatRoomResponse(10L, true);
        given(chatRoomService.createOrGetChatRoom(anyLong(), any())).willReturn(response);

        // when & then
        mockMvc.perform(post("/chat-rooms")
                        .header("Authorization", "Bearer test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.chatRoomId").value(10L))
                .andExpect(jsonPath("$.data.created").value(true))
                .andDo(MockMvcRestDocumentationWrapper.document(
                        "chat/create-chat-room",
                        resource(ResourceSnippetParameters.builder()
                                .tag("Chat")
                                .summary("채팅방 생성")
                                .description("상품 ID로 채팅방을 생성합니다. 이미 존재하는 채팅방이면 기존 방을 반환하고 200을 응답합니다. 신규 생성 시 201을 응답합니다.")
                                .requestHeaders(
                                        headerWithName("Authorization").description("Bearer JWT 액세스 토큰")
                                )
                                .requestSchema(Schema.schema("CreateChatRoomRequest"))
                                .responseSchema(Schema.schema("ChatRoomResponse"))
                                .requestFields(
                                        fieldWithPath("productId").type(JsonFieldType.NUMBER).description("채팅 대상 상품 ID")
                                )
                                .responseFields(
                                        fieldWithPath("code").type(JsonFieldType.STRING).description("응답 코드 (SUCCESS)"),
                                        fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지").optional(),
                                        fieldWithPath("data.chatRoomId").type(JsonFieldType.NUMBER).description("채팅방 ID"),
                                        fieldWithPath("data.created").type(JsonFieldType.BOOLEAN).description("신규 생성 여부 (true: 신규, false: 기존)")
                                )
                                .build()
                        )
                ));
    }

    @Test
    @DisplayName("내 채팅방 목록 조회 - GET /chat-rooms")
    @WithMockCustomUser
    void getMyChatRooms() throws Exception {
        // given
        List<ChatRoomSummaryResponse> response = List.of(
                new ChatRoomSummaryResponse(10L, 2L, "판매자닉네임", 1L, null, "안녕하세요", 2L)
        );
        given(chatRoomService.getMyChatRooms(anyLong())).willReturn(response);

        // when & then
        mockMvc.perform(get("/chat-rooms")
                        .header("Authorization", "Bearer test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data[0].chatRoomId").value(10L))
                .andDo(MockMvcRestDocumentationWrapper.document(
                        "chat/get-chat-rooms",
                        resource(ResourceSnippetParameters.builder()
                                .tag("Chat")
                                .summary("내 채팅방 목록 조회")
                                .description("구매자 또는 판매자로 참여한 채팅방 목록을 조회합니다.")
                                .requestHeaders(
                                        headerWithName("Authorization").description("Bearer JWT 액세스 토큰")
                                )
                                .responseSchema(Schema.schema("ChatRoomSummaryListResponse"))
                                .responseFields(
                                        fieldWithPath("code").type(JsonFieldType.STRING).description("응답 코드 (SUCCESS)"),
                                        fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지").optional(),
                                        fieldWithPath("data[].chatRoomId").type(JsonFieldType.NUMBER).description("채팅방 ID"),
                                        fieldWithPath("data[].opponentId").type(JsonFieldType.NUMBER).description("상대방 회원 ID"),
                                        fieldWithPath("data[].opponentNickname").type(JsonFieldType.STRING).description("상대방 닉네임"),
                                        fieldWithPath("data[].productId").type(JsonFieldType.NUMBER).description("상품 ID"),
                                        fieldWithPath("data[].opponentProfileImageUrl").type(JsonFieldType.STRING).description("상대방 프로필 이미지 URL").optional(),
                                        fieldWithPath("data[].lastMessage").type(JsonFieldType.STRING).description("마지막 메시지 내용").optional(),
                                        fieldWithPath("data[].unreadCount").type(JsonFieldType.NUMBER).description("읽지 않은 메시지 수")
                                )
                                .build()
                        )
                ));
    }

    @Test
    @DisplayName("채팅 메시지 목록 조회 - GET /chat-rooms/{chatRoomId}/messages")
    @WithMockCustomUser
    void getMessages() throws Exception {
        // given
        List<ChatMessageResponse> messages = List.of(
                new ChatMessageResponse(1L, 10L, 2L, "판매자닉네임", "안녕하세요", true, LocalDateTime.of(2024, 6, 1, 10, 0))
        );
        CursorPageResponse<ChatMessageResponse> response = new CursorPageResponse<>(messages, null, false);
        given(chatMessageService.getMessages(anyLong(), anyLong(), any(), anyInt())).willReturn(response);

        // when & then
        mockMvc.perform(get("/chat-rooms/{chatRoomId}/messages", 10L)
                        .header("Authorization", "Bearer test-token")
                        .param("size", "30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.content[0].messageId").value(1L))
                .andDo(MockMvcRestDocumentationWrapper.document(
                        "chat/get-messages",
                        resource(ResourceSnippetParameters.builder()
                                .tag("Chat")
                                .summary("채팅 메시지 목록 조회")
                                .description("채팅방의 메시지 이력을 커서 기반 페이지네이션으로 조회합니다.")
                                .requestHeaders(
                                        headerWithName("Authorization").description("Bearer JWT 액세스 토큰")
                                )
                                .pathParameters(
                                        parameterWithName("chatRoomId").description("채팅방 ID")
                                )
                                .queryParameters(
                                        parameterWithName("cursor").description("페이지 커서 — 이전 응답의 nextCursor 값 (첫 페이지는 생략)").optional(),
                                        parameterWithName("size").description("조회할 메시지 수 (기본값 30)").optional()
                                )
                                .responseSchema(Schema.schema("ChatMessageListResponse"))
                                .responseFields(
                                        fieldWithPath("code").type(JsonFieldType.STRING).description("응답 코드 (SUCCESS)"),
                                        fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지").optional(),
                                        fieldWithPath("data.content[].messageId").type(JsonFieldType.NUMBER).description("메시지 ID"),
                                        fieldWithPath("data.content[].chatRoomId").type(JsonFieldType.NUMBER).description("채팅방 ID"),
                                        fieldWithPath("data.content[].senderId").type(JsonFieldType.NUMBER).description("발신자 회원 ID"),
                                        fieldWithPath("data.content[].senderNickname").type(JsonFieldType.STRING).description("발신자 닉네임"),
                                        fieldWithPath("data.content[].content").type(JsonFieldType.STRING).description("메시지 내용"),
                                        fieldWithPath("data.content[].isRead").type(JsonFieldType.BOOLEAN).description("읽음 여부"),
                                        fieldWithPath("data.content[].createdAt").type(JsonFieldType.STRING).description("전송 일시"),
                                        fieldWithPath("data.nextCursor").type(JsonFieldType.STRING).description("다음 페이지 커서 (마지막 페이지면 null)").optional(),
                                        fieldWithPath("data.hasNext").type(JsonFieldType.BOOLEAN).description("다음 페이지 존재 여부")
                                )
                                .build()
                        )
                ));
    }

    @Test
    @DisplayName("채팅방 나가기 - DELETE /chat-rooms/{chatRoomId}/leave")
    @WithMockCustomUser
    void leaveChatRoom() throws Exception {
        // given
        doNothing().when(chatRoomService).leaveChatRoom(anyLong(), anyLong());

        // when & then
        mockMvc.perform(delete("/chat-rooms/{chatRoomId}/leave", 10L)
                        .header("Authorization", "Bearer test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andDo(MockMvcRestDocumentationWrapper.document(
                        "chat/leave-chat-room",
                        resource(ResourceSnippetParameters.builder()
                                .tag("Chat")
                                .summary("채팅방 나가기")
                                .description("채팅방을 나갑니다. 상대방의 채팅방은 유지됩니다.")
                                .requestHeaders(
                                        headerWithName("Authorization").description("Bearer JWT 액세스 토큰")
                                )
                                .pathParameters(
                                        parameterWithName("chatRoomId").description("채팅방 ID")
                                )
                                .responseSchema(Schema.schema("LeaveChatRoomResponse"))
                                .responseFields(
                                        fieldWithPath("code").type(JsonFieldType.STRING).description("응답 코드 (SUCCESS)"),
                                        fieldWithPath("message").type(JsonFieldType.STRING).description("채팅방을 성공적으로 나갔습니다.").optional()
                                )
                                .build()
                        )
                ));
    }

    @Test
    @DisplayName("메시지 읽음 처리 - PATCH /chat-rooms/{chatRoomId}/read")
    @WithMockCustomUser
    void markMessagesAsRead() throws Exception {
        // given
        doNothing().when(chatMessageService).markMessagesAsRead(anyLong(), anyLong());

        // when & then
        mockMvc.perform(patch("/chat-rooms/{chatRoomId}/read", 10L)
                        .header("Authorization", "Bearer test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andDo(MockMvcRestDocumentationWrapper.document(
                        "chat/mark-read",
                        resource(ResourceSnippetParameters.builder()
                                .tag("Chat")
                                .summary("메시지 읽음 처리")
                                .description("채팅방 입장 시 읽지 않은 메시지를 일괄 읽음 처리합니다.")
                                .requestHeaders(
                                        headerWithName("Authorization").description("Bearer JWT 액세스 토큰")
                                )
                                .pathParameters(
                                        parameterWithName("chatRoomId").description("채팅방 ID")
                                )
                                .responseSchema(Schema.schema("MarkReadResponse"))
                                .responseFields(
                                        fieldWithPath("code").type(JsonFieldType.STRING).description("응답 코드 (SUCCESS)"),
                                        fieldWithPath("message").type(JsonFieldType.STRING).description("메시지를 읽음 처리했습니다.").optional()
                                )
                                .build()
                        )
                ));
    }
}
