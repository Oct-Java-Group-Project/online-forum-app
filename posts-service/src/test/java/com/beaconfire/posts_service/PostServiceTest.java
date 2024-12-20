package com.beaconfire.posts_service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import com.beaconfire.posts_service.domain.Accessibility;
import com.beaconfire.posts_service.domain.Metadata;
import com.beaconfire.posts_service.domain.Post;
import com.beaconfire.posts_service.domain.PostReply;
import com.beaconfire.posts_service.domain.SubReply;
import com.beaconfire.posts_service.dto.DataResponse;
import com.beaconfire.posts_service.dto.PostWithUserDTO;
import com.beaconfire.posts_service.dto.UserDTO;
import com.beaconfire.posts_service.dto.UserPermissionsDTO;
import com.beaconfire.posts_service.exception.PostNotFoundException;
import com.beaconfire.posts_service.exception.ReplyNotFoundException;
import com.beaconfire.posts_service.feign.UserFeignClient;
import com.beaconfire.posts_service.repo.PostRepository;
import com.beaconfire.posts_service.service.PostService;
import com.fasterxml.jackson.databind.ObjectMapper;

class PostServiceTest {

    @Mock
    private PostRepository postRepository;

    @Mock
    private UserFeignClient userFeignClient;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private PostService postService;

    @BeforeEach
    void setUp() {
        userFeignClient = mock(UserFeignClient.class);
        postRepository = mock(PostRepository.class);
        objectMapper = new ObjectMapper();
        postService = new PostService(postRepository, userFeignClient, objectMapper);
    }

    
    

    @Test
    void getPostsByUserId_UserExists_ReturnsPostsWithUser() {
        // Arrange
        Integer userId = 1;
        Post post = new Post();
        post.setPostId("post1");
        post.setUserId(userId);
        List<Post> posts = List.of(post);

        UserDTO userDTO = new UserDTO();
        userDTO.setId(userId);
        userDTO.setFirstName("John");

        DataResponse userResponse = mock(DataResponse.class);
        when(userResponse.getSuccess()).thenReturn(true);
        when(userResponse.getData()).thenReturn(userDTO);

        when(postRepository.findByUserId(userId)).thenReturn(posts);
        when(userFeignClient.getUserById(userId)).thenReturn(userResponse);
        when(objectMapper.convertValue(userResponse.getData(), UserDTO.class)).thenReturn(userDTO);

        // Act
        List<PostWithUserDTO> result = postService.getPostsByUserId(userId);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("post1", result.get(0).getPost().getPostId());
        assertEquals("John", result.get(0).getUser().getFirstName());
        verify(postRepository, times(1)).findByUserId(userId);
        verify(userFeignClient, times(1)).getUserById(userId);
    }
    

    @Test
    void createPost_ValidUser_CreatesPost() {
        // Arrange
        Integer userId = 1;
        Post post = new Post();
        post.setUserId(userId);

        UserPermissionsDTO permissions = new UserPermissionsDTO();
        permissions.setActive(true);

        DataResponse mockResponse = mock(DataResponse.class);
        when(mockResponse.getSuccess()).thenReturn(true);
        when(mockResponse.getData()).thenReturn(permissions);

        when(userFeignClient.getUserById(userId)).thenReturn(mockResponse);
        when(objectMapper.convertValue(mockResponse.getData(), UserPermissionsDTO.class)).thenReturn(permissions);
        when(postRepository.save(any(Post.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Post result = postService.createPost(post);

        // Assert
        assertNotNull(result);
        assertNotNull(result.getPostId());
        verify(postRepository, times(1)).save(post);
    }

    @Test
    void createPost_InactiveUser_ThrowsException() {
        // Arrange
        Integer userId = 1;
        Post post = new Post();
        post.setUserId(userId);

        UserPermissionsDTO permissions = new UserPermissionsDTO();
        permissions.setActive(false);

        DataResponse mockResponse = mock(DataResponse.class);
        when(mockResponse.getSuccess()).thenReturn(true);
        when(mockResponse.getData()).thenReturn(permissions);

        when(userFeignClient.getUserById(userId)).thenReturn(mockResponse);
        when(objectMapper.convertValue(mockResponse.getData(), UserPermissionsDTO.class)).thenReturn(permissions);

        // Act & Assert
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> postService.createPost(post));
        assertEquals("User must verify their email to create a post.", exception.getMessage());
        verify(postRepository, never()).save(post);
    }

    @Test
    void likePost_PostExists_LikesPost() {
        // Arrange
        String postId = "post1";
        Integer userId = 1;

        Post post = new Post();
        Metadata metadata = new Metadata();
        metadata.setLikes(0);
        metadata.setLikesByUsers(new HashSet<>());

        post.setMetadata(metadata);

        when(postRepository.findById(postId)).thenReturn(Optional.of(post));
        when(postRepository.save(post)).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Post result = postService.likePost(postId, userId);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getMetadata().getLikes());
        assertTrue(result.getMetadata().getLikesByUsers().contains(userId));
        verify(postRepository, times(1)).findById(postId);
        verify(postRepository, times(1)).save(post);
    }

    @Test
    void unlikePost_PostExists_UnlikesPost() {
        // Arrange
        String postId = "post1";
        Integer userId = 1;

        Post post = new Post();
        Metadata metadata = new Metadata();
        metadata.setLikes(1);
        metadata.setLikesByUsers(new HashSet<>(Set.of(userId)));

        post.setMetadata(metadata);

        when(postRepository.findById(postId)).thenReturn(Optional.of(post));
        when(postRepository.save(post)).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Post result = postService.unlikePost(postId, userId);

        // Assert
        assertNotNull(result);
        assertEquals(0, result.getMetadata().getLikes());
        assertFalse(result.getMetadata().getLikesByUsers().contains(userId));
        verify(postRepository, times(1)).findById(postId);
        verify(postRepository, times(1)).save(post);
    }

    @Test
    void incrementViews_PostExists_IncrementsViewCount() {
        // Arrange
        String postId = "post1";

        Post post = new Post();
        Metadata metadata = new Metadata();
        metadata.setViews(5);
        post.setMetadata(metadata);

        when(postRepository.findById(postId)).thenReturn(Optional.of(post));
        when(postRepository.save(post)).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Post result = postService.incrementViews(postId);

        // Assert
        assertNotNull(result);
        assertEquals(6, result.getMetadata().getViews());
        verify(postRepository, times(1)).findById(postId);
        verify(postRepository, times(1)).save(post);
    }
    @Test
    void getPostById_NotFound() {
        String postId = "1";

        when(postRepository.findById(postId)).thenReturn(Optional.empty());

        Exception exception = assertThrows(PostNotFoundException.class, () -> postService.getPostById(postId));
        assertEquals("Post not found with ID: 1", exception.getMessage());
        verify(postRepository, times(1)).findById(postId);
    }
    @Test
    void updatePost_Unauthorized() {
        String postId = "1";
        Post updatedPost = new Post();
        updatedPost.setUserId(2); // Different user ID

        // Mock post
        Post post = new Post();
        post.setPostId(postId);
        post.setUserId(1); // Owner

        when(postRepository.findById(postId)).thenReturn(Optional.of(post));
        when(userFeignClient.getUserById(2)).thenReturn(
                DataResponse.builder().success(false).build()
        );

        Exception exception = assertThrows(IllegalStateException.class, () -> postService.updatePost(postId, updatedPost));
        assertEquals("User must verify their email to update a post.", exception.getMessage());
    }
    @Test
    void deletePost_Success() {
        // Arrange
        String postId = "1";
        Post post = new Post();
        post.setPostId(postId);
        post.setAccessibility(Accessibility.PUBLISHED);

        when(postRepository.findById(postId)).thenReturn(Optional.of(post));
        when(postRepository.save(any(Post.class))).thenReturn(post);

        // Act
        postService.deletePost(postId);

        // Assert
        verify(postRepository, times(1)).findById(postId);
        verify(postRepository, times(1)).save(post);
        assertEquals(Accessibility.DELETED, post.getAccessibility());
    }
    @Test
    void deletePost_PostNotFound() {
        // Arrange
        String postId = "1";
        when(postRepository.findById(postId)).thenReturn(Optional.empty());

        // Act & Assert
        PostNotFoundException exception = assertThrows(PostNotFoundException.class, () -> postService.deletePost(postId));
        assertEquals("Post not found with ID: " + postId, exception.getMessage());
        verify(postRepository, times(1)).findById(postId);
        verify(postRepository, never()).save(any(Post.class));
    }
    
    @Test
    void addReplyToPost_Success() {
        // Arrange
        String postId = "1";
        Integer userId = 123;

        Post post = new Post();
        post.setPostId(postId);
        post.setPostReplies(new ArrayList<>());

        PostReply reply = new PostReply();
        reply.setUserId(userId);
        reply.setComment("Test reply");

        UserPermissionsDTO permissions = UserPermissionsDTO.builder()
                .userId(userId)
                .active(true)
                .build();

        // Create DataResponse with UserPermissionsDTO as the data
        DataResponse mockResponse = DataResponse.builder()
                .success(true)
                .message("User found")
                .data(permissions)
                .build();

        when(postRepository.findById(postId)).thenReturn(Optional.of(post));
        when(userFeignClient.getUserById(userId)).thenReturn(mockResponse);
        when(postRepository.save(any(Post.class))).thenReturn(post);

        // Act
        Post updatedPost = postService.addReplyToPost(postId, reply);

        // Assert
        assertNotNull(updatedPost);
        assertEquals(1, updatedPost.getPostReplies().size());
        PostReply addedReply = updatedPost.getPostReplies().get(0);
        assertEquals("Test reply", addedReply.getComment());
        verify(postRepository, times(1)).findById(postId);
        verify(postRepository, times(1)).save(post);
    }


    
    @Test
    void addReplyToPost_PostNotFound() {
        // Arrange
        String postId = "1";
        PostReply reply = new PostReply();
        reply.setUserId(123);

        when(postRepository.findById(postId)).thenReturn(Optional.empty());

        // Act & Assert
        PostNotFoundException exception = assertThrows(PostNotFoundException.class, 
                () -> postService.addReplyToPost(postId, reply));
        assertEquals("Post not found with ID: " + postId, exception.getMessage());
        verify(postRepository, times(1)).findById(postId);
        verify(postRepository, never()).save(any(Post.class));
    }


    
    @Test
    void addReplyToPost_UserPermissionsInactive() {
        // Arrange
        String postId = "1";
        Integer userId = 123;

        Post post = new Post();
        post.setPostId(postId);

        PostReply reply = new PostReply();
        reply.setUserId(userId);

        UserPermissionsDTO permissions = UserPermissionsDTO.builder()
                .userId(userId)
                .active(false)
                .build();

        // Create DataResponse with UserPermissionsDTO as the data
        DataResponse mockResponse = DataResponse.builder()
                .success(true)
                .message("User found")
                .data(permissions)
                .build();

        when(postRepository.findById(postId)).thenReturn(Optional.of(post));
        when(userFeignClient.getUserById(userId)).thenReturn(mockResponse);

        // Act & Assert
        IllegalStateException exception = assertThrows(IllegalStateException.class, 
                () -> postService.addReplyToPost(postId, reply));
        assertEquals("User must verify their email to reply a post.", exception.getMessage());
        verify(postRepository, times(1)).findById(postId);
        verify(postRepository, never()).save(any(Post.class));
    }


    
    @Test
    void addReplyToPost_ExistingReplies() {
        // Arrange
        String postId = "1";
        Integer userId = 123;

        Post post = new Post();
        post.setPostId(postId);
        post.setPostReplies(new ArrayList<>());

        PostReply reply = new PostReply();
        reply.setUserId(userId);
        reply.setComment("Existing reply");

        // Mock user permissions
        UserPermissionsDTO permissions = UserPermissionsDTO.builder()
                .userId(userId)
                .active(true)
                .build();

        // Create DataResponse with UserPermissionsDTO as the data
        DataResponse mockResponse = DataResponse.builder()
                .success(true)
                .message("User found")
                .data(permissions)
                .build();

        // Mock post repository and userFeignClient
        when(postRepository.findById(postId)).thenReturn(Optional.of(post));
        when(userFeignClient.getUserById(userId)).thenReturn(mockResponse);
        when(postRepository.save(any(Post.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Post updatedPost = postService.addReplyToPost(postId, reply);

        // Assert
        assertNotNull(updatedPost);
        assertEquals(1, updatedPost.getPostReplies().size());
        PostReply addedReply = updatedPost.getPostReplies().get(0);
        assertEquals("Existing reply", addedReply.getComment());
        verify(postRepository, times(1)).findById(postId);
        verify(postRepository, times(1)).save(post);
    }
    @Test
    void addSubReplyToReply_Success() {
        // Arrange
        String postId = "1";
        String replyId = "r1";
        String subReplyContent = "This is a sub-reply.";

        Post post = new Post();
        post.setPostId(postId);

        PostReply reply = new PostReply();
        reply.setReplyId(replyId);
        reply.setSubReplies(new ArrayList<>());

        post.setPostReplies(List.of(reply));

        SubReply subReply = new SubReply();
        subReply.setComment(subReplyContent);

        when(postRepository.findById(postId)).thenReturn(Optional.of(post));
        when(postRepository.save(any(Post.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Post updatedPost = postService.addSubReplyToReply(postId, replyId, subReply);

        // Assert
        assertNotNull(updatedPost);
        PostReply updatedReply = updatedPost.getPostReplies().get(0);
        assertEquals(1, updatedReply.getSubReplies().size());
        assertEquals(subReplyContent, updatedReply.getSubReplies().get(0).getComment());
        verify(postRepository, times(1)).findById(postId);
        verify(postRepository, times(1)).save(post);
    }
    @Test
    void addSubReplyToReply_PostNotFound() {
        // Arrange
        String postId = "1";
        String replyId = "r1";
        SubReply subReply = new SubReply();

        when(postRepository.findById(postId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(PostNotFoundException.class, () -> postService.addSubReplyToReply(postId, replyId, subReply));
        verify(postRepository, times(1)).findById(postId);
        verify(postRepository, never()).save(any(Post.class));
    }
    @Test
    void addSubReplyToReply_ReplyNotFound() {
        // Arrange
        String postId = "1";
        String replyId = "r1";

        Post post = new Post();
        post.setPostId(postId);
        post.setPostReplies(new ArrayList<>()); // No replies

        SubReply subReply = new SubReply();

        when(postRepository.findById(postId)).thenReturn(Optional.of(post));

        // Act & Assert
        assertThrows(ReplyNotFoundException.class, () -> postService.addSubReplyToReply(postId, replyId, subReply));
        verify(postRepository, times(1)).findById(postId);
        verify(postRepository, never()).save(any(Post.class));
    }
    @Test
    void addSubReplyToReply_InitializeSubRepliesList() {
        // Arrange
        String postId = "1";
        String replyId = "r1";
        String subReplyContent = "New SubReply";

        Post post = new Post();
        post.setPostId(postId);

        PostReply reply = new PostReply();
        reply.setReplyId(replyId);
        reply.setSubReplies(null); // SubReplies list is null

        post.setPostReplies(List.of(reply));

        SubReply subReply = new SubReply();
        subReply.setComment(subReplyContent);

        when(postRepository.findById(postId)).thenReturn(Optional.of(post));
        when(postRepository.save(any(Post.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Post updatedPost = postService.addSubReplyToReply(postId, replyId, subReply);

        // Assert
        assertNotNull(updatedPost);
        PostReply updatedReply = updatedPost.getPostReplies().get(0);
        assertNotNull(updatedReply.getSubReplies());
        assertEquals(1, updatedReply.getSubReplies().size());
        assertEquals(subReplyContent, updatedReply.getSubReplies().get(0).getComment());
        verify(postRepository, times(1)).findById(postId);
        verify(postRepository, times(1)).save(post);
    }


    
    @Test
    void updateMetadata_PostNotFound() {
        // Arrange
        String postId = "1";
        Metadata newMetadata = new Metadata();

        when(postRepository.findById(postId)).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> postService.updateMetadata(postId, newMetadata));

        assertEquals("Post not found with ID: " + postId, exception.getMessage());
        verify(postRepository, times(1)).findById(postId);
        verify(postRepository, never()).save(any(Post.class));
    }

    @Test
    void updateMetadata_PartialUpdate() {
        // Arrange
        String postId = "1";
        Metadata partialMetadata = new Metadata();
        partialMetadata.setLikes(10); // Update only likes
        partialMetadata.setViews(100); // Views must also be explicitly updated for testing

        Metadata originalMetadata = new Metadata(0, 5, new HashSet<>(), new Date(), new Date(), new Date());

        Post existingPost = new Post();
        existingPost.setPostId(postId);
        existingPost.setMetadata(originalMetadata);

        when(postRepository.findById(postId)).thenReturn(Optional.of(existingPost));
        when(postRepository.save(any(Post.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Post updatedPost = postService.updateMetadata(postId, partialMetadata);

        // Assert
        assertNotNull(updatedPost);
        assertEquals(100, updatedPost.getMetadata().getViews()); // Ensure views are updated
        assertEquals(10, updatedPost.getMetadata().getLikes());  // Ensure likes are updated
        verify(postRepository, times(1)).findById(postId);
        verify(postRepository, times(1)).save(existingPost);
    }

    
    @Test
    void updateMetadata_VerifyIntegrity() {
        // Arrange
        String postId = "1";
        Metadata updatedMetadata = new Metadata(200, 30, Set.of(5, 6), new Date(), new Date(), new Date());

        Metadata originalMetadata = new Metadata(100, 20, Set.of(1, 2, 3), new Date(0), new Date(0), new Date(0));

        Post existingPost = new Post();
        existingPost.setPostId(postId);
        existingPost.setMetadata(originalMetadata);

        when(postRepository.findById(postId)).thenReturn(Optional.of(existingPost));
        when(postRepository.save(any(Post.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Post updatedPost = postService.updateMetadata(postId, updatedMetadata);

        // Assert
        assertNotNull(updatedPost);
        assertEquals(Set.of(5, 6), updatedPost.getMetadata().getLikesByUsers());
        assertNotEquals(Set.of(1, 2, 3), updatedPost.getMetadata().getLikesByUsers());
        verify(postRepository, times(1)).findById(postId);
        verify(postRepository, times(1)).save(existingPost);
    }
    
    @Test
    public void testEnumValues_AccessAllConstants() {
        // Assert that all enum constants are accessible
        assertEquals("UNPUBLISHED", Accessibility.UNPUBLISHED.name());
        assertEquals("PUBLISHED", Accessibility.PUBLISHED.name());
        assertEquals("HIDDEN", Accessibility.HIDDEN.name());
        assertEquals("BANNED", Accessibility.BANNED.name());
        assertEquals("DELETED", Accessibility.DELETED.name());
    }

    @Test
    public void testFromValue_ValidInput() {
        // Assert that fromValue parses valid inputs correctly
        assertEquals(Accessibility.UNPUBLISHED, Accessibility.fromValue("unpublished"));
        assertEquals(Accessibility.PUBLISHED, Accessibility.fromValue("PUBLISHED"));
        assertEquals(Accessibility.HIDDEN, Accessibility.fromValue("Hidden"));
        assertEquals(Accessibility.BANNED, Accessibility.fromValue("bAnNeD"));
        assertEquals(Accessibility.DELETED, Accessibility.fromValue("deleted"));
    }

    @Test
    public void testFromValue_InvalidInput() {
        // Assert that fromValue throws IllegalArgumentException for invalid inputs
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            Accessibility.fromValue("INVALID");
        });
        assertTrue(exception.getMessage().contains("No enum constant com.beaconfire.posts_service.domain.Accessibility.INVALID"));
    }

    @Test
    public void testFromValue_NullInput() {
        // Assert that fromValue throws NullPointerException for null input
        assertThrows(NullPointerException.class, () -> Accessibility.fromValue(null));
    }

    @Test
    public void testEnumToString() {
        // Assert that the toString method returns the correct string representation
        assertEquals("UNPUBLISHED", Accessibility.UNPUBLISHED.toString());
        assertEquals("PUBLISHED", Accessibility.PUBLISHED.toString());
        assertEquals("HIDDEN", Accessibility.HIDDEN.toString());
        assertEquals("BANNED", Accessibility.BANNED.toString());
        assertEquals("DELETED", Accessibility.DELETED.toString());
    }

@Test
public void testUpdateReply_Success() {
    // Arrange
    String postId = "123";
    String replyId = "456";
    Post existingPost = new Post();
    existingPost.setPostId(postId);

    PostReply existingReply = new PostReply();
    existingReply.setReplyId(replyId);
    existingReply.setComment("Old comment");

    List<PostReply> replies = new ArrayList<>();
    replies.add(existingReply);
    existingPost.setPostReplies(replies);

    PostReply updatedReply = new PostReply();
    updatedReply.setComment("Updated comment");

    when(postRepository.findById(postId)).thenReturn(Optional.of(existingPost));
    when(postRepository.save(any(Post.class))).thenReturn(existingPost);

    // Act
    Post result = postService.updateReply(postId, replyId, updatedReply);

    // Assert
    assertNotNull(result);
    assertEquals(1, result.getPostReplies().size());
    assertEquals("Updated comment", result.getPostReplies().get(0).getComment());
    assertNotNull(result.getPostReplies().get(0).getUpdated_at());
    verify(postRepository, times(1)).findById(postId);
    verify(postRepository, times(1)).save(existingPost);
}

@Test
public void testUpdateReply_PostNotFound() {
    // Arrange
    String postId = "123";
    String replyId = "456";
    PostReply updatedReply = new PostReply();
    updatedReply.setComment("Updated comment");

    when(postRepository.findById(postId)).thenReturn(Optional.empty());

    // Act & Assert
    assertThrows(PostNotFoundException.class, () -> postService.updateReply(postId, replyId, updatedReply));
    verify(postRepository, times(1)).findById(postId);
    verify(postRepository, never()).save(any(Post.class));
}

@Test
public void testUpdateReply_ReplyNotFound() {
    // Arrange
    String postId = "123";
    String replyId = "456";
    Post existingPost = new Post();
    existingPost.setPostId(postId);
    existingPost.setPostReplies(new ArrayList<>());

    PostReply updatedReply = new PostReply();
    updatedReply.setComment("Updated comment");

    when(postRepository.findById(postId)).thenReturn(Optional.of(existingPost));

    // Act & Assert
    assertThrows(PostNotFoundException.class, () -> postService.updateReply(postId, replyId, updatedReply));
    verify(postRepository, times(1)).findById(postId);
    verify(postRepository, never()).save(any(Post.class));

}
@Test
public void testUpdateSubReply_Success() {
    // Arrange
    String postId = "123";
    String replyId = "456";
    String subReplyId = "789";

    Post existingPost = new Post();
    existingPost.setPostId(postId);

    PostReply existingReply = new PostReply();
    existingReply.setReplyId(replyId);

    SubReply existingSubReply = new SubReply();
    existingSubReply.setSubReplyId(subReplyId);
    existingSubReply.setComment("Old sub-reply comment");

    List<SubReply> subReplies = new ArrayList<>();
    subReplies.add(existingSubReply);
    existingReply.setSubReplies(subReplies);

    List<PostReply> replies = new ArrayList<>();
    replies.add(existingReply);
    existingPost.setPostReplies(replies);

    SubReply updatedSubReply = new SubReply();
    updatedSubReply.setComment("Updated sub-reply comment");

    when(postRepository.findById(postId)).thenReturn(Optional.of(existingPost));
    when(postRepository.save(any(Post.class))).thenReturn(existingPost);

    // Act
    Post result = postService.updateSubReply(postId, replyId, subReplyId, updatedSubReply);

    // Assert
    assertNotNull(result);
    assertEquals(1, result.getPostReplies().size());
    assertEquals(1, result.getPostReplies().get(0).getSubReplies().size());
    assertEquals("Updated sub-reply comment", result.getPostReplies().get(0).getSubReplies().get(0).getComment());
    assertNotNull(result.getPostReplies().get(0).getSubReplies().get(0).getUpdated_at());
    verify(postRepository, times(1)).findById(postId);
    verify(postRepository, times(1)).save(existingPost);
}

@Test
public void testUpdateSubReply_PostNotFound() {
    // Arrange
    String postId = "123";
    String replyId = "456";
    String subReplyId = "789";

    SubReply updatedSubReply = new SubReply();
    updatedSubReply.setComment("Updated sub-reply comment");

    when(postRepository.findById(postId)).thenReturn(Optional.empty());

    // Act & Assert
    assertThrows(PostNotFoundException.class, () -> postService.updateSubReply(postId, replyId, subReplyId, updatedSubReply));
    verify(postRepository, times(1)).findById(postId);
    verify(postRepository, never()).save(any(Post.class));
}

@Test
public void testUpdateSubReply_ReplyNotFound() {
    // Arrange
    String postId = "123";
    String replyId = "456";
    String subReplyId = "789";

    Post existingPost = new Post();
    existingPost.setPostId(postId);
    existingPost.setPostReplies(new ArrayList<>());

    SubReply updatedSubReply = new SubReply();
    updatedSubReply.setComment("Updated sub-reply comment");

    when(postRepository.findById(postId)).thenReturn(Optional.of(existingPost));

    // Act & Assert
    assertThrows(PostNotFoundException.class, () -> postService.updateSubReply(postId, replyId, subReplyId, updatedSubReply));
    verify(postRepository, times(1)).findById(postId);
    verify(postRepository, never()).save(any(Post.class));
}

@Test
public void testUpdateSubReply_SubReplyNotFound() {
    // Arrange
    String postId = "123";
    String replyId = "456";
    String subReplyId = "789";

    Post existingPost = new Post();
    existingPost.setPostId(postId);

    PostReply existingReply = new PostReply();
    existingReply.setReplyId(replyId);
    existingReply.setSubReplies(new ArrayList<>());

    List<PostReply> replies = new ArrayList<>();
    replies.add(existingReply);
    existingPost.setPostReplies(replies);

    SubReply updatedSubReply = new SubReply();
    updatedSubReply.setComment("Updated sub-reply comment");

    when(postRepository.findById(postId)).thenReturn(Optional.of(existingPost));

    // Act & Assert
    assertThrows(PostNotFoundException.class, () -> postService.updateSubReply(postId, replyId, subReplyId, updatedSubReply));
    verify(postRepository, times(1)).findById(postId);
    verify(postRepository, never()).save(any(Post.class));
}


}





