package hs.mediasystem.plugin.library.scene;

import hs.mediasystem.runner.LessLoader;

import java.util.List;

import javafx.scene.Node;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

@Singleton
public class EntityLayout {
  @Inject private Provider<EntityPresentation> presentationProvider;
  @Inject private List<Layout<EntityPresentation, ?>> layouts;

  public EntityPresentation createPresentation() {
    return presentationProvider.get();
  }

  //@Override
  public Node createView(EntityPresentation presentation) {
    EntityView node = new EntityView();

    node.backgroundPane.backdropProperty().bindBidirectional(presentation.backdrop);
    node.getStylesheets().add(LessLoader.compile(getClass().getResource("styles.css")).toExternalForm());

    presentation.location.addListener((obs, o, n) -> locationChanged(n, node, presentation));

    return node;
  }

  // Presentation, has model, can be used by multiple views
  // Layout, creates views and glues them to a presentation, singleton
  // View, dumb, handles animations and placement
  // Behaviour, part of layout, decides how a view is configured

  public enum Area {
    CENTER_TOP,
    CENTER,
    NAVIGATION,
    NAME,
    DETAILS,
    INFORMATION_PANEL,
    CONTEXT_PANEL,  // Panel that quickly fades in on the left depending on the presence of content
    PREVIEW_PANEL   // Panel that slides in from the right depending on the presence of content
  }

  private Layout<EntityPresentation, ?> currentLayout;

  private void locationChanged(LibraryLocation location, EntityView view, EntityPresentation presentation) {
    // -> Simple call to find View
    // -> If same View, do nothing, let locationChanged on that View handle it
    // -> Otherwise replace


    Layout<EntityPresentation, ?> layout = layouts.stream().filter(s -> s.getLocationClass().isInstance(location)).findFirst().orElse(null);

    if(currentLayout != layout) {
      currentLayout = layout;

      view.setCenter(layout.createView(presentation).getNode());
      view.requestFocus();
    }
  }

    /*
     * Should convert Productions / MediaStreams into the same class with fields: --> DONE: ProductionItem
     *
     * - Production
     * - MediaStream (optional)
     *
     * Note that MovieDescriptor lacks the info to play the title... stream comes from MediaStream, previously copied into Media object
     *
     * Thinking that it may be good to cache minimal information with Streams (like the Production info only) and fetch the rest as
     * needed... basically only "sorting / filtering" information should be stored / fetched right away, so it depends on what we
     * want to sort and filter on:
     *
     * - Watched status
     * - Popularity
     * - Date added
     * - Rating
     * - Genre
     * - Title (alpha)
     * - Release date
     * - Tags (mainly user ones, but could include TMDB tags)
     *
     * Rest of info is fetched later (and potentially cached with a generic mechanism).
     */

}
