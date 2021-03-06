package hs.mediasystem.ext.tmdb.movie;

import com.fasterxml.jackson.databind.JsonNode;

import hs.mediasystem.domain.stream.MediaType;
import hs.mediasystem.ext.basicmediatypes.domain.Identifier;
import hs.mediasystem.ext.basicmediatypes.domain.PersonRole;
import hs.mediasystem.ext.basicmediatypes.services.RolesQueryService;
import hs.mediasystem.ext.tmdb.PersonRoles;
import hs.mediasystem.ext.tmdb.TheMovieDatabase;

import java.io.IOException;
import java.util.List;

import javax.inject.Inject;

public class TmdbRolesQueryService implements RolesQueryService {
  @Inject private TheMovieDatabase tmdb;
  @Inject private PersonRoles personRoles;

  @Override
  public String getDataSourceName() {
    return "TMDB";
  }

  @Override
  public List<PersonRole> query(Identifier identifier) throws IOException {
    JsonNode info = tmdb.query(identifierToLocation(identifier), "text:json:" + identifier);

    return personRoles.toPersonRoles(info);
  }

  private static String identifierToLocation(Identifier identifier) {
    if(identifier.getDataSource().getType() == MediaType.MOVIE) {
      return "3/movie/" + identifier.getId() + "/credits";
    }
    if(identifier.getDataSource().getType() == MediaType.SERIE) {
      return "3/tv/" + identifier.getId() + "/credits";
    }
    if(identifier.getDataSource().getType() == MediaType.EPISODE) {
      String[] parts = identifier.getId().split("/");

      return "3/tv/" + parts[0] + "/season/" + parts[1] + "/episode/" + parts[2] + "/credits";
    }

    throw new IllegalArgumentException("Unsupported identifier: " + identifier);
  }
}
