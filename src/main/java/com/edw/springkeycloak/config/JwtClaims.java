/**
 * JwtClaims.java
 * @author Indika Munaweera {indika.k@creativesoftware.com}
 * @since Mar 24, 2019
 */
package com.edw.springkeycloak.config;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@AllArgsConstructor
@Getter
@Setter
@ToString
public class JwtClaims {
  private String id;
  private String subject;
  private List<String> roles;
  private List<String> authorities;
}
