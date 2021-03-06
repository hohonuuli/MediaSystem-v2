package hs.mediasystem.ext.tmdb.movie;

import com.fasterxml.jackson.databind.JsonNode;

import hs.mediasystem.domain.stream.MediaType;
import hs.mediasystem.ext.basicmediatypes.domain.Identifier;
import hs.mediasystem.ext.basicmediatypes.domain.Production;
import hs.mediasystem.ext.basicmediatypes.domain.ProductionIdentifier;
import hs.mediasystem.ext.basicmediatypes.services.RecommendationQueryService;
import hs.mediasystem.ext.tmdb.ObjectFactory;
import hs.mediasystem.ext.tmdb.TheMovieDatabase;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

public class TmdbRecommendationQueryService implements RecommendationQueryService {
  @Inject private TheMovieDatabase tmdb;
  @Inject private ObjectFactory objectFactory;

  @Override
  public List<Production> query(ProductionIdentifier identifier) throws IOException {
    JsonNode info = tmdb.query(identifierToLocation(identifier), "text:json:" + identifier);
    List<Production> productions = new ArrayList<>();

    for(JsonNode result : info.path("results")) {
      if(identifier.getDataSource().getType() == MediaType.MOVIE) {
        productions.add(objectFactory.toMovie(result));
      }
      else if(identifier.getDataSource().getType() == MediaType.SERIE) {
        productions.add(objectFactory.toSerie(result, null));
      }
    }

    return productions;
  }

  private static String identifierToLocation(Identifier identifier) {
    if(identifier.getDataSource().getType() == MediaType.MOVIE) {
      return "3/movie/" + identifier.getId() + "/recommendations";
    }
    if(identifier.getDataSource().getType() == MediaType.SERIE) {
      return "3/tv/" + identifier.getId() + "/recommendations";
    }

    throw new IllegalArgumentException("Unsupported identifier: " + identifier);
  }
}
