🚀 JReleaser {{projectVersion}} has been released! {{releaseNotesUrl}}
🐞 This is a patch release that only includes bug fixes in the following areas: docker, java-archive, native-image, jlink
🐳 Patched couple of buildx issues as well as when `docker.io` was used as a registry name instead of DEFAULT
☕️ java-archive failed to generate a suitable launcher for a modular application
📦 assemblers can now specify a timestamp for all archive entries (reproducible builds FTW) as well as longFileMode/bigNumberMode for tars
🏆 deprecated NATIVE_IMAGE distribution type was accidentally removed. Use BINARY instead
🔗 targets set in jlink.jdeps will be automatically converted to absolute paths
📝 Full list of changes available at the {{milestoneName}} milestone {{projectLinkVcsBrowser}}/milestones?state=closed
🙏 As always, feedback is welcome. Feel free to raise a ticket at {{projectLinkBugTracker}}