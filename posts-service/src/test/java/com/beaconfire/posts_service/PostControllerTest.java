package com.beaconfire.posts_service;



import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;

import com.beaconfire.posts_service.controller.PostController;
import com.beaconfire.posts_service.domain.Accessibility;
import com.beaconfire.posts_service.domain.Metadata;
import com.beaconfire.posts_service.domain.Post;
import com.beaconfire.posts_service.domain.PostReply;
import com.beaconfire.posts_service.domain.SubReply;
import com.beaconfire.posts_service.dto.AccessibilityRequest;
import com.beaconfire.posts_service.dto.DataResponse;
import com.beaconfire.posts_service.dto.PostWithUserDTO;
import com.beaconfire.posts_service.dto.UserDTO;
import com.beaconfire.posts_service.exception.PostNotFoundException;
import com.beaconfire.posts_service.exception.ReplyNotFoundException;
import com.beaconfire.posts_service.service.PostService;

class PostControllerTest {

    @Mock
    private PostService postService;

    @InjectMocks
    private PostController postController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
//        postController = new PostController(postService);
    }



    @Test
    void updatePost_Success() {
        // Arrange
        Post updatedPost = new Post();
        updatedPost.setPostId("1");
        updatedPost.setTitle("Updated Post");
        updatedPost.setContent("Updated content");
        updatedPost.setUserId(1);
        updatedPost.setAccessibility(Accessibility.PUBLISHED);

        UserDTO userDTO = new UserDTO();
        userDTO.setId(1);
        userDTO.setEmail("user@example.com");
        userDTO.setFirstName("John");
        userDTO.setLastName("Doe");

        PostWithUserDTO updatedPostWithUserDTO = PostWithUserDTO.builder()
                .post(updatedPost)
                .user(userDTO)
                .build();

        when(postService.updatePost("1", updatedPost)).thenReturn(updatedPostWithUserDTO);

        BindingResult bindingResult = Mockito.mock(BindingResult.class);
        when(bindingResult.hasErrors()).thenReturn(false); // Simulate no validation errors

        // Act
        DataResponse response = postController.updatePost("1", updatedPost, bindingResult);

        // Assert
        assertNotNull(response);
        assertEquals(true, response.getSuccess());
        assertEquals("Post updated successfully", response.getMessage());
        assertEquals(updatedPostWithUserDTO, response.getData());
        verify(postService, times(1)).updatePost("1", updatedPost);
    }
    


    @Test
    void deletePost_Success() {
        // Act
        DataResponse response = postController.deletePost("1");

        // Assert
        assertNotNull(response);
        assertEquals(true, response.getSuccess());
        assertEquals("Post deleted successfully", response.getMessage());
        verify(postService, times(1)).deletePost("1");
    }

    @Test
    void getAllPosts_Success() {
        // Arrange
        PostWithUserDTO postWithUserDTO = new PostWithUserDTO();
        postWithUserDTO.setPost(new Post());

        when(postService.getAllPosts()).thenReturn(List.of(postWithUserDTO));

        // Act
        DataResponse response = postController.getAllPosts();

        // Assert
        assertNotNull(response);
        assertEquals(true, response.getSuccess());
        assertEquals("Posts retrieved successfully", response.getMessage());
        assertEquals(1, ((List<?>) response.getData()).size());
        verify(postService, times(1)).getAllPosts();
    }

    @Test
    void getPostsByUserId_Success() {
        // Arrange
        PostWithUserDTO postWithUserDTO = new PostWithUserDTO();
        postWithUserDTO.setPost(new Post());

        when(postService.getPostsByUserId(1)).thenReturn(List.of(postWithUserDTO));

        // Act
        DataResponse response = postController.getPostsWithUserByUserId(1);

        // Assert
        assertNotNull(response);
        assertEquals(true, response.getSuccess());
        assertEquals("Posts fetched successfully for user ID: 1", response.getMessage());
        assertEquals(1, ((List<?>) response.getData()).size());
        verify(postService, times(1)).getPostsByUserId(1);
    }

    @Test
    void updateAccessibility_Success() {
        // Arrange
        Post updatedPost = new Post();
        updatedPost.setPostId("1");
        updatedPost.setAccessibility(Accessibility.PUBLISHED);

        AccessibilityRequest request = AccessibilityRequest.builder()
                .accessibility("PUBLISHED")
                .currentUserId(1)
                .build();

        when(postService.updateAccessibility("1", Accessibility.PUBLISHED, 1)).thenReturn(updatedPost);

        // Act
        DataResponse response = postController.updateAccessibility("1", request);

        // Assert
        assertNotNull(response);
        assertEquals(true, response.getSuccess());
        assertEquals("Accessibility updated successfully.", response.getMessage());
        assertEquals(updatedPost, response.getData());
        verify(postService, times(1)).updateAccessibility("1", Accessibility.PUBLISHED, 1);
    }
    @Test
    void testGetPostsByAccessibility_ValidAccessibility() {
        // Arrange
        List<Post> posts = List.of(new Post());
        when(postService.getPostsByAccessibility(Accessibility.PUBLISHED)).thenReturn(posts);

        // Act
        DataResponse response = postController.getPostsByAccessibility("PUBLISHED");

        // Assert
        assertNotNull(response);
        assertTrue(response.getSuccess());
        assertEquals("Posts retrieved successfully for accessibility: PUBLISHED", response.getMessage());
        assertEquals(posts, response.getData());
        verify(postService, times(1)).getPostsByAccessibility(Accessibility.PUBLISHED);
    }

    @Test
    void testGetPostsByAccessibility_InvalidAccessibility() {
        // Act
        DataResponse response = postController.getPostsByAccessibility("INVALID");

        // Assert
        assertNotNull(response);
        assertFalse(response.getSuccess());
        assertTrue(response.getMessage().contains("Invalid accessibility value. Accepted values are:"));
        assertEquals(
                String.join(", ", EnumSet.allOf(Accessibility.class).stream().map(Enum::name).toList()),
                response.getMessage().split(": ")[1]
        );
        assertNull(response.getData());
        verifyNoInteractions(postService);
    }

    @Test
    void testGetPostsByAccessibility_UnexpectedError() {
        // Arrange
        when(postService.getPostsByAccessibility(Accessibility.PUBLISHED)).thenThrow(new RuntimeException("Unexpected error"));

        // Act
        DataResponse response = postController.getPostsByAccessibility("PUBLISHED");

        // Assert
        assertNotNull(response);
        assertFalse(response.getSuccess());
        assertTrue(response.getMessage().contains("An unexpected error occurred:"));
        assertNull(response.getData());
        verify(postService, times(1)).getPostsByAccessibility(Accessibility.PUBLISHED);
    }

    @Test
    void testGetPostsByAccessibility_NoPosts() {
        // Arrange
        when(postService.getPostsByAccessibility(Accessibility.HIDDEN)).thenReturn(Collections.emptyList());

        // Act
        DataResponse response = postController.getPostsByAccessibility("HIDDEN");

        // Assert
        assertNotNull(response);
        assertTrue(response.getSuccess());
        assertEquals("Posts retrieved successfully for accessibility: HIDDEN", response.getMessage());
        assertTrue(((List<?>) response.getData()).isEmpty());
        verify(postService, times(1)).getPostsByAccessibility(Accessibility.HIDDEN);
    }
    @Test
    void testUpdateMetadata_Success() {
        // Arrange
        String postId = "123";
        Metadata metadata = new Metadata();
        metadata.setViews(100);
        metadata.setLikes(50);
        
        Post updatedPost = new Post();
        updatedPost.setPostId(postId);
        updatedPost.setMetadata(metadata);

        when(postService.updateMetadata(postId, metadata)).thenReturn(updatedPost);

        // Act
        DataResponse response = postController.updateMetadata(postId, metadata);

        // Assert
        assertNotNull(response);
        assertTrue(response.getSuccess());
        assertEquals("Metadata updated successfully", response.getMessage());
        assertEquals(updatedPost, response.getData());
        verify(postService, times(1)).updateMetadata(postId, metadata);
    }

    @Test
    void testUpdateMetadata_PostNotFound() {
        // Arrange
        String postId = "123";
        Metadata metadata = new Metadata();
        when(postService.updateMetadata(postId, metadata)).thenThrow(new PostNotFoundException("Post not found with ID: " + postId));

        // Act
        DataResponse response = postController.updateMetadata(postId, metadata);

        // Assert
        assertNotNull(response);
        assertFalse(response.getSuccess());
        assertEquals("Post not found with ID: " + postId, response.getMessage());
        assertNull(response.getData());
        verify(postService, times(1)).updateMetadata(postId, metadata);
    }

    @Test
    void testUpdateMetadata_UnexpectedError() {
        // Arrange
        String postId = "123";
        Metadata metadata = new Metadata();
        when(postService.updateMetadata(postId, metadata)).thenThrow(new RuntimeException("Unexpected error"));

        // Act
        DataResponse response = postController.updateMetadata(postId, metadata);

        // Assert
        assertNotNull(response);
        assertFalse(response.getSuccess());
        assertTrue(response.getMessage().contains("An unexpected error occurred: Unexpected error"));
        assertNull(response.getData());
        verify(postService, times(1)).updateMetadata(postId, metadata);
    }
    @Test
    void testLikePost_Success() {
        // Arrange
        String postId = "123";
        Integer userId = 1;

        Metadata metadata = new Metadata();
        metadata.setLikes(0); // Initialize likes to zero

        Post updatedPost = new Post();
        updatedPost.setPostId(postId);
        updatedPost.setMetadata(metadata);

        when(postService.likePost(postId, userId)).thenReturn(updatedPost);

        // Act
        ResponseEntity<DataResponse> response = postController.likePost(postId, userId);

        // Assert
        assertNotNull(response);
        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().getSuccess());
        assertEquals("Post liked successfully.", response.getBody().getMessage());
        assertEquals(updatedPost, response.getBody().getData());
        verify(postService, times(1)).likePost(postId, userId);
    }


    @Test
    void testLikePost_DuplicateLike() {
        // Arrange
        String postId = "123";
        Integer userId = 1;
        when(postService.likePost(postId, userId)).thenThrow(new RuntimeException("User has already liked this post."));

        // Act
        ResponseEntity<DataResponse> response = postController.likePost(postId, userId);

        // Assert
        assertNotNull(response);
        assertEquals(400, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().getSuccess());
        assertEquals("User has already liked this post.", response.getBody().getMessage());
        assertNull(response.getBody().getData());
        verify(postService, times(1)).likePost(postId, userId);
    }

    @Test
    void testLikePost_PostNotFound() {
        // Arrange
        String postId = "999";
        Integer userId = 1;
        when(postService.likePost(postId, userId)).thenThrow(new RuntimeException("Post not found."));

        // Act
        ResponseEntity<DataResponse> response = postController.likePost(postId, userId);

        // Assert
        assertNotNull(response);
        assertEquals(400, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().getSuccess());
        assertEquals("Post not found.", response.getBody().getMessage());
        assertNull(response.getBody().getData());
        verify(postService, times(1)).likePost(postId, userId);
    }

    @Test
    void testLikePost_InvalidUserId() {
        // Act
        ResponseEntity<DataResponse> response = postController.likePost("123", null);

        // Assert
        assertNotNull(response);
        assertEquals(400, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().getSuccess());
        assertEquals("Invalid user ID provided.", response.getBody().getMessage());
    }
    @Test
    void testUnlikePost_Success() {
        // Arrange
        String postId = "123";
        Integer userId = 1;

        Metadata metadata = new Metadata();
        metadata.setLikes(5); // Assume post has 5 likes initially

        Post updatedPost = new Post();
        updatedPost.setPostId(postId);
        updatedPost.setMetadata(metadata);

        when(postService.unlikePost(postId, userId)).thenReturn(updatedPost);

        // Act
        ResponseEntity<DataResponse> response = postController.unlikePost(postId, userId);

        // Assert
        assertNotNull(response);
        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().getSuccess());
        assertEquals("Post unliked successfully.", response.getBody().getMessage());
        assertEquals(updatedPost, response.getBody().getData());
        verify(postService, times(1)).unlikePost(postId, userId);
    }
    @Test
    void testUnlikePost_InvalidUserId() {
        // Act
        ResponseEntity<DataResponse> response = postController.unlikePost("123", null);

        // Assert
        assertNotNull(response);
        assertEquals(400, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().getSuccess());
        assertEquals("Invalid user ID provided.", response.getBody().getMessage());
    }
    @Test
    void testUnlikePost_RuntimeException() {
        // Arrange
        String postId = "123";
        Integer userId = 1;

        when(postService.unlikePost(postId, userId)).thenThrow(new RuntimeException("Post not found"));

        // Act
        ResponseEntity<DataResponse> response = postController.unlikePost(postId, userId);

        // Assert
        assertNotNull(response);
        assertEquals(400, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().getSuccess());
        assertEquals("Post not found", response.getBody().getMessage());
        assertNull(response.getBody().getData());
    }
    @Test
    void testIncrementPostViews_Success() {
        // Arrange
        String postId = "123";

        Metadata metadata = new Metadata();
        metadata.setViews(10); // Assume post has 10 views initially

        Post updatedPost = new Post();
        updatedPost.setPostId(postId);
        updatedPost.setMetadata(metadata);

        when(postService.incrementViews(postId)).thenReturn(updatedPost);

        // Act
        ResponseEntity<DataResponse> response = postController.incrementPostViews(postId);

        // Assert
        assertNotNull(response);
        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().getSuccess());
        assertEquals("Post view count incremented successfully.", response.getBody().getMessage());
        assertEquals(10, response.getBody().getData()); // Updated view count
        verify(postService, times(1)).incrementViews(postId);
    }
    @Test
    void testIncrementPostViews_InvalidPostId() {
        // Arrange
        String invalidPostId = "invalid-post-id";

        when(postService.incrementViews(invalidPostId)).thenThrow(new RuntimeException("Post not found"));

        // Act
        ResponseEntity<DataResponse> response = postController.incrementPostViews(invalidPostId);

        // Assert
        assertNotNull(response);
        assertEquals(400, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().getSuccess());
        assertEquals("Post not found", response.getBody().getMessage());
        assertNull(response.getBody().getData());
        verify(postService, times(1)).incrementViews(invalidPostId);
    }
    @Test
    void testIncrementPostViews_NullPostId() {
        // Act
        ResponseEntity<DataResponse> response = postController.incrementPostViews(null);

        // Assert
        assertNotNull(response);
        assertEquals(400, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().getSuccess());
        assertEquals("Invalid post ID provided.", response.getBody().getMessage());
        assertNull(response.getBody().getData());
    }
    @Test
    void testAddReplyToPost_Success() {
        // Arrange
        String postId = "123";

        PostReply reply = new PostReply();
        reply.setReplyId("1");
        reply.setComment("This is a reply");
        reply.setUserId(1);

        Post updatedPost = new Post();
        updatedPost.setPostId(postId);
        updatedPost.setPostReplies(List.of(reply));

        when(postService.addReplyToPost(postId, reply)).thenReturn(updatedPost);

        // Act
        DataResponse response = postController.addReplyToPost(postId, reply);

        // Assert
        assertNotNull(response);
        assertTrue(response.getSuccess());
        assertEquals("Replied successfully", response.getMessage());
        assertEquals(updatedPost, response.getData());
        verify(postService, times(1)).addReplyToPost(postId, reply);
    }
    @Test
    void testAddReplyToPost_PostNotFound() {
        // Arrange
        String invalidPostId = "nonexistent-post";
        PostReply reply = new PostReply();
        reply.setReplyId("1");
        reply.setComment("This is a reply");
        reply.setUserId(1);

        when(postService.addReplyToPost(invalidPostId, reply)).thenThrow(new PostNotFoundException("Post not found"));

        // Act
        DataResponse response = postController.addReplyToPost(invalidPostId, reply);

        // Assert
        assertNotNull(response);
        assertFalse(response.getSuccess());
        assertEquals("Post not found", response.getMessage());
        assertNull(response.getData());
        verify(postService, times(1)).addReplyToPost(invalidPostId, reply);
    }
    @Test
    void testAddReplyToPost_NullComment() {
        // Arrange
        String postId = "123";

        // Act
        DataResponse response = postController.addReplyToPost(postId, null);

        // Assert
        assertNotNull(response);
        assertFalse(response.getSuccess());
        assertEquals("Comment cannot be null", response.getMessage());
        assertNull(response.getData());
    }

    @Test
    void testAddReplyToPost_ExceptionThrown() {
        // Arrange
        String postId = "123";
        PostReply reply = new PostReply();
        reply.setReplyId("1");
        reply.setComment("This is a reply");
        reply.setUserId(1);

        when(postService.addReplyToPost(postId, reply)).thenThrow(new RuntimeException("Database error"));

        // Act
        DataResponse response = postController.addReplyToPost(postId, reply);

        // Assert
        assertNotNull(response);
        assertFalse(response.getSuccess());
        assertEquals("An unexpected error occurred: Database error", response.getMessage());
        assertNull(response.getData());
        verify(postService, times(1)).addReplyToPost(postId, reply);
    }
    @Test
    void testAddSubReplyToReply_Success() {
        // Arrange
        String postId = "123";
        String replyId = "456";
        SubReply subReply = new SubReply();
        subReply.setComment("This is a sub-reply");

        Post updatedPost = new Post();
        when(postService.addSubReplyToReply(postId, replyId, subReply)).thenReturn(updatedPost);

        // Act
        DataResponse response = postController.addSubReplyToReply(postId, replyId, subReply);

        // Assert
        assertNotNull(response);
        assertTrue(response.getSuccess());
        assertEquals("Sub-reply added successfully", response.getMessage());
        assertEquals(updatedPost, response.getData());
        verify(postService, times(1)).addSubReplyToReply(postId, replyId, subReply);
    }

    @Test
    void testAddSubReplyToReply_NullSubReply() {
        // Arrange
        String postId = "123";
        String replyId = "456";

        // Act
        DataResponse response = postController.addSubReplyToReply(postId, replyId, null);

        // Assert
        assertNotNull(response);
        assertFalse(response.getSuccess());
        assertEquals("Sub-reply cannot be null", response.getMessage());
        assertNull(response.getData());
        verify(postService, never()).addSubReplyToReply(anyString(), anyString(), any(SubReply.class));
    }


    @Test
    void testAddSubReplyToReply_PostNotFoundException() {
        // Arrange
        String postId = "123";
        String replyId = "456";
        SubReply subReply = new SubReply();
        subReply.setComment("This is a sub-reply");

        when(postService.addSubReplyToReply(postId, replyId, subReply))
                .thenThrow(new PostNotFoundException("Post not found"));

        // Act
        DataResponse response = postController.addSubReplyToReply(postId, replyId, subReply);

        // Assert
        assertNotNull(response);
        assertFalse(response.getSuccess());
        assertEquals("Post not found", response.getMessage());
        assertNull(response.getData());
        verify(postService, times(1)).addSubReplyToReply(postId, replyId, subReply);
    }

    @Test
    void testAddSubReplyToReply_UnexpectedError() {
        // Arrange
        String postId = "123";
        String replyId = "456";
        SubReply subReply = new SubReply();
        subReply.setComment("This is a sub-reply");

        when(postService.addSubReplyToReply(postId, replyId, subReply))
                .thenThrow(new RuntimeException("Unexpected error"));

        // Act
        DataResponse response = postController.addSubReplyToReply(postId, replyId, subReply);

        // Assert
        assertNotNull(response);
        assertFalse(response.getSuccess());
        assertEquals("An unexpected error occurred: Unexpected error", response.getMessage());
        assertNull(response.getData());
        verify(postService, times(1)).addSubReplyToReply(postId, replyId, subReply);
    }
    @Test
    void testUpdateReply_Success() {
        // Arrange
        String postId = "123";
        String replyId = "456";
        PostReply updatedReply = new PostReply();
        updatedReply.setComment("Updated content");
        updatedReply.setUserId(1);

        Post updatedPost = new Post();
        updatedPost.setPostId(postId);

        when(postService.updateReply(postId, replyId, updatedReply)).thenReturn(updatedPost);

        // Act
        DataResponse response = postController.updateReply(postId, replyId, updatedReply);

        // Assert
        assertNotNull(response);
        assertTrue(response.getSuccess());
        assertEquals("Reply updated successfully", response.getMessage());
        assertEquals(updatedPost, response.getData());
        verify(postService, times(1)).updateReply(postId, replyId, updatedReply);
    }

    @Test
    void testUpdateReply_PostNotFound() {
        // Arrange
        String postId = "123";
        String replyId = "456";
        PostReply updatedReply = new PostReply();
        updatedReply.setComment("Updated content");
        updatedReply.setUserId(1);

        when(postService.updateReply(postId, replyId, updatedReply)).thenThrow(new PostNotFoundException("Post not found"));

        // Act
        DataResponse response = postController.updateReply(postId, replyId, updatedReply);

        // Assert
        assertNotNull(response);
        assertFalse(response.getSuccess());
        assertEquals("Error: Post not found", response.getMessage());
        assertNull(response.getData());
        verify(postService, times(1)).updateReply(postId, replyId, updatedReply);
    }

    @Test
    void testUpdateReply_UnexpectedError() {
        // Arrange
        String postId = "123";
        String replyId = "456";
        PostReply updatedReply = new PostReply();
        updatedReply.setComment("Updated content");
        updatedReply.setUserId(1);

        when(postService.updateReply(postId, replyId, updatedReply)).thenThrow(new RuntimeException("Unexpected error"));

        // Act
        DataResponse response = postController.updateReply(postId, replyId, updatedReply);

        // Assert
        assertNotNull(response);
        assertFalse(response.getSuccess());
        assertEquals("An unexpected error occurred: Unexpected error", response.getMessage());
        assertNull(response.getData());
        verify(postService, times(1)).updateReply(postId, replyId, updatedReply);
    }



    @Test
    void testSoftDeleteReply_Success() {
        // Arrange
        String postId = "123";
        String replyId = "456";
        Integer currentUserId = 1;

        // Act
        DataResponse response = postController.softDeleteReply(postId, replyId, currentUserId);

        // Assert
        assertNotNull(response);
        assertTrue(response.getSuccess());
        assertEquals("Reply has been soft-deleted successfully.", response.getMessage());
        assertNull(response.getData());
        verify(postService, times(1)).softDeleteReply(postId, replyId, currentUserId);
    }

    @Test
    void testSoftDeleteReply_ReplyNotFound() {
        // Arrange
        String postId = "123";
        String replyId = "456";
        Integer currentUserId = 1;

        doThrow(new ReplyNotFoundException("Reply not found"))
                .when(postService).softDeleteReply(postId, replyId, currentUserId);

        // Act
        DataResponse response = postController.softDeleteReply(postId, replyId, currentUserId);

        // Assert
        assertNotNull(response);
        assertFalse(response.getSuccess());
        assertEquals("Reply not found: Reply not found", response.getMessage());
        assertNull(response.getData());
        verify(postService, times(1)).softDeleteReply(postId, replyId, currentUserId);
    }

    @Test
    void testSoftDeleteReply_Unauthorized() {
        // Arrange
        String postId = "123";
        String replyId = "456";
        Integer currentUserId = 2;

        doThrow(new RuntimeException("Unauthorized action"))
                .when(postService).softDeleteReply(postId, replyId, currentUserId);

        // Act
        DataResponse response = postController.softDeleteReply(postId, replyId, currentUserId);

        // Assert
        assertNotNull(response);
        assertFalse(response.getSuccess());
        assertEquals("Unauthorized: Unauthorized action", response.getMessage());
        assertNull(response.getData());
        verify(postService, times(1)).softDeleteReply(postId, replyId, currentUserId);
    }

    @Test
    void testSoftDeleteReply_UnexpectedError() {
        // Arrange
        String postId = "123";
        String replyId = "456";
        int currentUserId = 789;

        doThrow(new RuntimeException("Unexpected error"))
            .when(postService).softDeleteReply(postId, replyId, currentUserId);

        // Act
        DataResponse response = postController.softDeleteReply(postId, replyId, currentUserId);

        // Assert
        assertNotNull(response);
        assertFalse(response.getSuccess());
        assertEquals("Unauthorized: Unexpected error", response.getMessage());
    }
    
    @Test
    void testUpdateSubReply_Success() {
        // Arrange
        SubReply updatedSubReply = new SubReply("201", 1, "Updated comment", false, new Date(), new Date());
        PostReply postReply = new PostReply("101", 1, "Sample reply", false, new Date(), new Date(), List.of(updatedSubReply));
        Post updatedPost = new Post("1", "Sample Post", "Sample Content", false, 1, Accessibility.PUBLISHED, null, new Date(), new Date(), null, null, List.of(postReply), null);

        when(postService.updateSubReply("1", "101", "201", updatedSubReply)).thenReturn(updatedPost);

        // Act
        DataResponse response = postController.updateSubReply("1", "101", "201", updatedSubReply);

        // Assert
        assertTrue(response.getSuccess());
        assertEquals("Sub-reply updated successfully", response.getMessage());
        assertNotNull(response.getData());
        Post resultPost = (Post) response.getData();
        assertEquals("1", resultPost.getPostId());
        assertEquals("Sample Post", resultPost.getTitle());
        assertEquals("Updated comment", resultPost.getPostReplies().get(0).getSubReplies().get(0).getComment());
    }

    @Test
    void testUpdateSubReply_PostNotFound() {
        // Arrange
        SubReply updatedSubReply = new SubReply("201", 1, "Updated comment", false, new Date(), new Date());

        when(postService.updateSubReply("1", "101", "201", updatedSubReply))
                .thenThrow(new PostNotFoundException("Post with ID 1 not found"));

        // Act
        DataResponse response = postController.updateSubReply("1", "101", "201", updatedSubReply);

        // Assert
        assertFalse(response.getSuccess());
        assertEquals("Error: Post with ID 1 not found", response.getMessage());
        assertNull(response.getData());
    }

    @Test
    void testUpdateSubReply_GenericException() {
        // Arrange
        SubReply updatedSubReply = new SubReply("201", 1, "Updated comment", false, new Date(), new Date());

        when(postService.updateSubReply("1", "101", "201", updatedSubReply))
                .thenThrow(new RuntimeException("Unexpected error"));

        // Act
        DataResponse response = postController.updateSubReply("1", "101", "201", updatedSubReply);

        // Assert
        assertFalse(response.getSuccess());
        assertEquals("An unexpected error occurred: Unexpected error", response.getMessage());
        assertNull(response.getData());
    }
    























    
    
}
    
    


