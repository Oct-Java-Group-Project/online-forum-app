package com.beaconfire.posts_service.domain;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class Metadata {

    private int views;
    private int likes;
    private Set<Integer> likesByUsers = new HashSet<>();
    private Date createdAt;
    private Date updatedAt;
    private Date lastActivityAt;


}

