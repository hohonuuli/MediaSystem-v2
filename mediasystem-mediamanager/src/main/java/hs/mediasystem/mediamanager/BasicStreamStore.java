package hs.mediasystem.mediamanager;

import hs.mediasystem.ext.basicmediatypes.Identification;
import hs.mediasystem.ext.basicmediatypes.Identifier;
import hs.mediasystem.scanner.api.BasicStream;
import hs.mediasystem.scanner.api.MediaType;
import hs.mediasystem.scanner.api.StreamID;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public interface BasicStreamStore {

  /*
   * Look up functions
   */

  Optional<BasicStream> findStream(StreamID streamId);
  StreamSource findStreamSource(StreamID streamId);
  Map<Identifier, Identification> findIdentifications(StreamID streamId);
  Set<BasicStream> findStreams(Identifier identifier);
  Set<BasicStream> findStreams(MediaType type, String tag);
  Map<BasicStream, Map<Identifier, Identification>> findIdentifiersByStreams(MediaType type, String tag);
  Optional<StreamID> findParentId(StreamID streamId);
  List<BasicStream> findNewest(int maximum);
}
