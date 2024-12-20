package com.beaconfire.posts_service.controller;

import java.util.EnumSet;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.beaconfire.posts_service.domain.Accessibility;
import com.beaconfire.posts_service.domain.Metadata;
import com.beaconfire.posts_service.domain.Post;
import com.beaconfire.posts_service.domain.PostReply;
import com.beaconfire.posts_service.domain.SubReply;
import com.beaconfire.posts_service.dto.AccessibilityRequest;
import com.beaconfire.posts_service.dto.DataResponse;
import com.beaconfire.posts_service.dto.PostWithUserDTO;
import com.beaconfire.posts_service.exception.InvalidAccessibilityException;
import com.beaconfire.posts_service.exception.PostNotFoundException;
import com.beaconfire.posts_service.exception.ReplyNotFoundException;
import com.beaconfire.posts_service.service.PostService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/posts")
@Tag(name = "Post Service", description = "Endpoints for managing posts")

public class PostController {

   
    private final PostService postService;
    public PostController(PostService postService) {
    	this.postService=postService;
    }
    private void validateAccessibility(Accessibility accessibility) {
        if (accessibility == null) {
            throw new InvalidAccessibilityException("Accessibility must be provided and cannot be null.");
        }


        boolean isValid = EnumSet.allOf(Accessibility.class).contains(accessibility);
        if (!isValid) {
            throw new InvalidAccessibilityException("Invalid accessibility value: " + accessibility);
        }
    }
    
//    @PostMapping
//    public ResponseEntity<Post> createPost(@Valid @RequestBody Post postRequest) {
//        Post createdPost = postService.createPost(postRequest);
//        return new ResponseEntity<>(createdPost, HttpStatus.CREATED);
//    }

    @Operation(summary = "Create a new post", description = "Creates a new post with the given details.")
    @PostMapping
    public DataResponse createPost(@Valid @RequestBody Post post, BindingResult result) {
        if (result.hasErrors()) {
            // Collect validation error messages
            String errorMessage = result.getFieldErrors().stream()
                    .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                    .reduce((msg1, msg2) -> msg1 + ", " + msg2)
                    .orElse("Validation failed");

            // Return validation error response
            return DataResponse.builder()
                    .success(false)
                    .message(errorMessage)
                    .data(null)
                    .build();
        }

        try {
            // Validate accessibility
            validateAccessibility(post.getAccessibility());

            // Save the post
            Post createdPost = postService.createPost(post);
            return DataResponse.builder()
                    .success(true)
                    .message("Post created successfully")
                    .data(createdPost)
                    .build();
        } catch (InvalidAccessibilityException ex) {
            return DataResponse.builder()
                    .success(false)
                    .message(ex.getMessage())
                    .data(null)
                    .build();
        } catch (Exception ex) {
            return DataResponse.builder()
                    .success(false)
                    .message("An unexpected error occurred: " + ex.getMessage())
                    .data(null)
                    .build();
        }
    }
    @Operation(summary = "Update an existing post", description = "Updates the details of an existing post by its ID.")
    @PutMapping("/{postId}")
    public DataResponse updatePost(@PathVariable String postId, @Valid @RequestBody Post updatedPost, BindingResult result) {
        if (result.hasErrors()) {
            // Collect validation error messages
            String errorMessage = result.getFieldErrors().stream()
                    .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                    .reduce((msg1, msg2) -> msg1 + ", " + msg2)
                    .orElse("Validation failed");

            // Return validation error response
            return DataResponse.builder()
                    .success(false)
                    .message(errorMessage)
                    .data(null)
                    .build();
        }

        try {
            // Validate accessibility
            validateAccessibility(updatedPost.getAccessibility());

            // Update the post
            PostWithUserDTO updatedPostEntity = postService.updatePost(postId, updatedPost);
            return DataResponse.builder()
                    .success(true)
                    .message("Post updated successfully")
                    .data(updatedPostEntity)
                    .build();
        } catch (PostNotFoundException ex) {
            return DataResponse.builder()
                    .success(false)
                    .message(ex.getMessage())
                    .data(null)
                    .build();
        } catch (InvalidAccessibilityException ex) {
            return DataResponse.builder()
                    .success(false)
                    .message(ex.getMessage())
                    .data(null)
                    .build();
        } catch (Exception ex) {
            return DataResponse.builder()
                    .success(false)
                    .message("An unexpected error occurred: " + ex.getMessage())
                    .data(null)
                    .build();
        }
    }
    @Operation(summary = "Get top 3 posts by user", description = "Fetches the top 3 posts of a user, sorted by the number of replies in descending order")
    @GetMapping("/user/{userId}/top3")
    public ResponseEntity<DataResponse> getTop3PostsByUser(@PathVariable Integer userId) {
        List<Post> topPosts = postService.getTop3PostsByUser(userId);

        return ResponseEntity.ok(
                DataResponse.builder()
                        .success(true)
                        .message("Top 3 posts retrieved successfully for user ID: " + userId)
                        .data(topPosts)
                        .build()
        );
    }


    @Operation(summary = "Delete a post", description = "Deletes a post by its ID.")
    @DeleteMapping("/{postId}")
    public DataResponse deletePost(@PathVariable String postId) {
   	try {
    	postService.deletePost(postId);
        return DataResponse.builder()
                .success(true)
                .message("Post deleted successfully")
                .data(null)
                .build();
    
    }catch (PostNotFoundException ex) {
        // Handle case where the post is not found
        return DataResponse.builder()
                .success(false)
                .message(ex.getMessage())
                .data(null)
                .build();
    } catch (Exception ex) {
        // Handle any unexpected errors
        return DataResponse.builder()
                .success(false)
                .message("An unexpected error occurred: " + ex.getMessage())
                .data(null)
                .build();
    }
}
    @Operation(summary = "Get all posts", description = "Retrieves a list of all posts.")
    @GetMapping
    public DataResponse getAllPosts() {
        List<PostWithUserDTO > posts = postService.getAllPosts();
        return DataResponse.builder()
                .success(true)
                .message("Posts retrieved successfully")
                .data(posts)
                .build();
    }

    @Operation(summary = "Get posts by user ID", description = "Retrieves all posts created by a specific user.")
    @GetMapping("/user/{userId}")
    public DataResponse getPostsWithUserByUserId(
            @PathVariable Integer userId) {
        List<PostWithUserDTO> postsWithUser = postService.getPostsByUserId(userId);

        return  DataResponse.builder()
                     .success(true)
                     .message("Posts fetched successfully for user ID: " + userId)
                     .data(postsWithUser)
                     .build();
     
    }
    
    @Operation(summary = "Get posts by accessibility", description = "Retrieves posts filtered by accessibility status.")
    @GetMapping("/accessibility/{accessibility}")
    public DataResponse getPostsByAccessibility(@PathVariable String accessibility) {
        try {
            Accessibility parsedAccessibility = Accessibility.valueOf(accessibility.toUpperCase());
            List<Post> posts = postService.getPostsByAccessibility(parsedAccessibility);
            return DataResponse.builder()
                    .success(true)
                    .message("Posts retrieved successfully for accessibility: " + parsedAccessibility)
                    .data(posts)
                    .build();
        } catch (IllegalArgumentException ex) {
            return DataResponse.builder()
                    .success(false)
                    .message("Invalid accessibility value. Accepted values are: " +
                            String.join(", ", EnumSet.allOf(Accessibility.class).stream().map(Enum::name).toList()))
                    .data(null)
                    .build();
        } catch (Exception ex) {
            return DataResponse.builder()
                    .success(false)
                    .message("An unexpected error occurred: " + ex.getMessage())
                    .data(null)
                    .build();
        }
    }
    @Operation(summary = "Update post accessibility", description = "Updates the accessibility status of a post by its ID. Admins can ban/unban posts; Normal users can hide posts.")
    @PatchMapping("/{postId}/accessibility")
    public DataResponse updateAccessibility(
            @PathVariable String postId,
            @RequestBody AccessibilityRequest request) {
        try {
            // Convert the accessibility string to an enum value
            Accessibility parsedAccessibility = Accessibility.valueOf(request.getAccessibility().toUpperCase());

            // Update the accessibility of the post
            Post updatedPost = postService.updateAccessibility(postId, parsedAccessibility, request.getCurrentUserId());

            return DataResponse.builder()
                    .success(true)
                    .message("Accessibility updated successfully.")
                    .data(updatedPost)
                    .build();
        } catch (IllegalArgumentException ex) {
            // Handle invalid accessibility values
            return DataResponse.builder()
                    .success(false)
                    .message("Invalid accessibility value. Accepted values are: " +
                            String.join(", ", EnumSet.allOf(Accessibility.class).stream().map(Enum::name).toList()))
                    .data(null)
                    .build();
        } catch (PostNotFoundException ex) {
            // Handle case where the post is not found
            return DataResponse.builder()
                    .success(false)
                    .message(ex.getMessage())
                    .data(null)
                    .build();
        } catch (RuntimeException ex) {
            // Handle unauthorized access
            return DataResponse.builder()
                    .success(false)
                    .message(ex.getMessage())
                    .data(null)
                    .build();
        } catch (Exception ex) {
            // Handle any unexpected errors
            return DataResponse.builder()
                    .success(false)
                    .message("An unexpected error occurred: " + ex.getMessage())
                    .data(null)
                    .build();
        }
    }







    @Operation(summary = "Get post by ID", description = "Retrieves a post by its ID.")
    @GetMapping("/{postId}")
    public DataResponse getPostById(@PathVariable String postId) {
        try {
            // Attempt to retrieve the post
        	PostWithUserDTO postWithUser = postService.getPostById(postId);
            return DataResponse.builder()
                    .success(true)
                    .message("Post retrieved successfully")
                    .data(postWithUser)
                    .build();
        } catch (PostNotFoundException ex) {
            // Handle case where the post is not found
            return DataResponse.builder()
                    .success(false)
                    .message(ex.getMessage())
                    .data(null)
                    .build();
        } catch (Exception ex) {
            // Handle any unexpected errors
            return DataResponse.builder()
                    .success(false)
                    .message("An unexpected error occurred: " + ex.getMessage())
                    .data(null)
                    .build();
        }
    }

    @Operation(summary = "Update post metadata", description = "Updates the metadata of a post by its ID.")
    @PatchMapping("/{postId}/metadata")
    public DataResponse updateMetadata(@PathVariable String postId, @RequestBody Metadata metadata) {
    	try {
    	Post updatedPost = postService.updateMetadata(postId, metadata);
    	return DataResponse.builder()
                .success(true)
                .message("Metadata updated successfully")
                .data(updatedPost)
                .build();
    } catch (PostNotFoundException ex) {
        // Handle case where the post is not found
        return DataResponse.builder()
                .success(false)
                .message(ex.getMessage())
                .data(null)
                .build();
    } catch (Exception ex) {
        // Handle any unexpected errors
        return DataResponse.builder()
                .success(false)
                .message("An unexpected error occurred: " + ex.getMessage())
                .data(null)
                .build();
    }
}
    @Operation(summary = "Add a reply to a post", description = "Adds a new reply to a specified post.")
    @PostMapping("/{postId}/replies")
    public DataResponse addReplyToPost(@PathVariable String postId, @RequestBody PostReply comment) {
    	try {
    		if (comment == null) {
                return DataResponse.builder()
                        .success(false)
                        .message("Comment cannot be null")
                        .data(null)
                        .build();
            }
    	Post updatedPost = postService.addReplyToPost(postId, comment);
    	return DataResponse.builder()
                .success(true)
                .message("Replied successfully")
                .data(updatedPost)
                .build();
    } catch (PostNotFoundException ex) {
        // Handle case where the post is not found
        return DataResponse.builder()
                .success(false)
                .message(ex.getMessage())
                .data(null)
                .build();
    } catch (Exception ex) {
        // Handle any unexpected errors
        return DataResponse.builder()
                .success(false)
                .message("An unexpected error occurred: " + ex.getMessage())
                .data(null)
                .build();
    }
  }
    
    @Operation(summary = "Add a sub-reply to a reply", description = "Adds a sub-reply to a specific reply in a post.")
    @PostMapping("/{postId}/replies/{replyId}/subreplies")
    public DataResponse addSubReplyToReply(
            @PathVariable String postId,
            @PathVariable String replyId,
            @RequestBody SubReply subReply) {
        try {
            if (subReply == null) {
                return DataResponse.builder()
                        .success(false)
                        .message("Sub-reply cannot be null")
                        .data(null)
                        .build();
            }
            Post updatedPost = postService.addSubReplyToReply(postId, replyId, subReply);
            return DataResponse.builder()
                    .success(true)
                    .message("Sub-reply added successfully")
                    .data(updatedPost)
                    .build();
        } catch (PostNotFoundException ex) {
            return DataResponse.builder()
                    .success(false)
                    .message(ex.getMessage())
                    .data(null)
                    .build();
        } catch (Exception ex) {
            return DataResponse.builder()
                    .success(false)
                    .message("An unexpected error occurred: " + ex.getMessage())
                    .data(null)
                    .build();
        }
    }

    @Operation(summary = "Update a reply", description = "Updates a specific reply in a post.")
    @PutMapping("/{postId}/replies/{replyId}")
    public DataResponse updateReply(
            @PathVariable String postId,
            @PathVariable String replyId,
            @RequestBody @Valid PostReply updatedReply) {
        try {
            // Attempt to update the reply
            Post updatedPost = postService.updateReply(postId, replyId, updatedReply);
            return DataResponse.builder()
                    .success(true)
                    .message("Reply updated successfully")
                    .data(updatedPost)
                    .build();
        } catch (PostNotFoundException ex) {
            return DataResponse.builder()
                    .success(false)
                    .message("Error: " + ex.getMessage())
                    .data(null)
                    .build();
        } catch (Exception ex) {
            return DataResponse.builder()
                    .success(false)
                    .message("An unexpected error occurred: " + ex.getMessage())
                    .data(null)
                    .build();
        }
    }
    @Operation(summary = "Update a sub-reply", description = "Updates a specific sub-reply of a reply in a post.")
    @PutMapping("/{postId}/replies/{replyId}/subreplies/{subReplyId}")
    public DataResponse updateSubReply(
            @PathVariable String postId,
            @PathVariable String replyId,
            @PathVariable String subReplyId,
            @RequestBody @Valid SubReply updatedSubReply) {
        try {
            // Attempt to update the sub-reply
            Post updatedPost = postService.updateSubReply(postId, replyId, subReplyId, updatedSubReply);
            return DataResponse.builder()
                    .success(true)
                    .message("Sub-reply updated successfully")
                    .data(updatedPost)
                    .build();
        } catch (PostNotFoundException ex) {
            return DataResponse.builder()
                    .success(false)
                    .message("Error: " + ex.getMessage())
                    .data(null)
                    .build();
        } catch (Exception ex) {
            return DataResponse.builder()
                    .success(false)
                    .message("An unexpected error occurred: " + ex.getMessage())
                    .data(null)
                    .build();
        }
    }
    @Operation(summary = "Soft delete a reply", description = "Soft deletes a reply by marking it as deleted. Only the post owner or an admin can perform this action.")
    @DeleteMapping("/{postId}/replies/{replyId}/delete")
    public DataResponse softDeleteReply(
            @PathVariable String postId,
            @PathVariable String replyId,
            @RequestParam Integer currentUserId) {
        try {
            postService.softDeleteReply(postId, replyId, currentUserId);
            return DataResponse.builder()
                    .success(true)
                    .message("Reply has been soft-deleted successfully.")
                    .data(null)
                    .build();
        } catch (ReplyNotFoundException ex) {
            return DataResponse.builder()
                    .success(false)
                    .message("Reply not found: " + ex.getMessage())
                    .data(null)
                    .build();
        } catch (RuntimeException ex) {
            return DataResponse.builder()
                    .success(false)
                    .message("Unauthorized: " + ex.getMessage())
                    .data(null)
                    .build();
        } catch (Exception ex) {
            return DataResponse.builder()
                    .success(false)
                    .message("An unexpected error occurred: " + ex.getMessage())
                    .data(null)
                    .build();
        }
    }
    
    @Operation(summary = "Soft delete a sub-reply", description = "Soft deletes a sub-reply by marking it as deleted. Only the post owner or an admin can perform this action.")
    @DeleteMapping("/{postId}/replies/{replyId}/sub-replies/{subReplyId}/delete")
    public DataResponse softDeleteSubReply(
            @PathVariable String postId,
            @PathVariable String replyId,
            @PathVariable String subReplyId,
            @RequestParam Integer currentUserId) {
        try {
            postService.softDeleteSubReply(postId, replyId, subReplyId, currentUserId);
            return DataResponse.builder()
                    .success(true)
                    .message("Sub-reply has been soft-deleted successfully.")
                    .data(null)
                    .build();
        } catch (ReplyNotFoundException ex) {
            return DataResponse.builder()
                    .success(false)
                    .message("Sub-reply not found: " + ex.getMessage())
                    .data(null)
                    .build();
        } catch (RuntimeException ex) {
            return DataResponse.builder()
                    .success(false)
                    .message("Unauthorized: " + ex.getMessage())
                    .data(null)
                    .build();
        } catch (Exception ex) {
            return DataResponse.builder()
                    .success(false)
                    .message("An unexpected error occurred: " + ex.getMessage())
                    .data(null)
                    .build();
        }
    }
    @Operation(
            summary = "Like a Post",
            description = "Allows a user to like a post. The user's ID is tracked to ensure proper functionality."
    )
    @PatchMapping("/{postId}/like")
    public ResponseEntity<DataResponse> likePost(
            @PathVariable String postId,
            @RequestParam(required = false) Integer userId) {
        if (userId == null) {
            return ResponseEntity.badRequest().body(
                    DataResponse.builder()
                            .success(false)
                            .message("Invalid user ID provided.")
                            .data(null)
                            .build());
        }
        try {
            Post updatedPost = postService.likePost(postId, userId);
            return ResponseEntity.ok(
                    DataResponse.builder()
                            .success(true)
                            .message("Post liked successfully.")
                            .data(updatedPost)
                            .build());
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(
                    DataResponse.builder()
                            .success(false)
                            .message(ex.getMessage())
                            .data(null)
                            .build());
        }
    }


    @Operation(
            summary = "Unlike a Post",
            description = "Allows a user to unlike a post. The user's ID is tracked to ensure proper functionality."
    )
    @PatchMapping("/{postId}/unlike")
    public ResponseEntity<DataResponse> unlikePost(
            @PathVariable String postId,
            @RequestParam Integer userId) {
    	if (userId == null) {
    	    return ResponseEntity.badRequest().body(
    	            DataResponse.builder()
    	                    .success(false)
    	                    .message("Invalid user ID provided.")
    	                    .data(null)
    	                    .build());
    	}

        try {
            Post updatedPost = postService.unlikePost(postId, userId);
            return ResponseEntity.ok(
                    DataResponse.builder()
                            .success(true)
                            .message("Post unliked successfully.")
                            .data(updatedPost)
                            .build());
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(
                    DataResponse.builder()
                            .success(false)
                            .message(ex.getMessage())
                            .data(null)
                            .build());
        }
    }
    
    @Operation(
            summary = "Increment Post Views",
            description = "Increments the view count for a post whenever it is accessed."
    )

    @PatchMapping("/{postId}/views")
    public ResponseEntity<DataResponse> incrementPostViews(@PathVariable String postId) {
    	if (postId == null || postId.isEmpty()) {
    	    return ResponseEntity.badRequest().body(
    	            DataResponse.builder()
    	                    .success(false)
    	                    .message("Invalid post ID provided.")
    	                    .data(null)
    	                    .build());
    	}

    	try {
            Post updatedPost = postService.incrementViews(postId);
            return ResponseEntity.ok(
                    DataResponse.builder()
                            .success(true)
                            .message("Post view count incremented successfully.")
                            .data(updatedPost.getMetadata().getViews())
                            .build());
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(
                    DataResponse.builder()
                            .success(false)
                            .message(ex.getMessage())
                            .data(null)
                            .build());
        }            
     }
    
    
    


}
